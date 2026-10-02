package com.college.library.service;

import com.college.library.dto.FineDtos.*;
import com.college.library.dto.PageResponse;
import com.college.library.entity.*;
import com.college.library.exception.BusinessRuleException;
import com.college.library.exception.ResourceNotFoundException;
import com.college.library.mapper.CirculationMapper;
import com.college.library.repository.*;
import com.college.library.security.CurrentUserService;
import com.college.library.util.TextUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fines and (mock) fine payments. No payment gateway is used: the librarian records
 * money received at the counter by cash, UPI, card or other means.
 */
@Service
public class FineService {

    private final FineRepository fineRepository;
    private final FinePaymentRepository paymentRepository;
    private final ReturnRecordRepository returnRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public FineService(FineRepository fineRepository, FinePaymentRepository paymentRepository,
                       ReturnRecordRepository returnRepository, AuditService auditService, CurrentUserService currentUser) {
        this.fineRepository = fineRepository;
        this.paymentRepository = paymentRepository;
        this.returnRepository = returnRepository;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public PageResponse<FineResponse> search(String q, FineStatus status, Long memberId, Pageable pageable) {
        Page<Fine> page = fineRepository.findAll(SearchSpecs.fines(q, status, memberId), pageable);
        return PageResponse.of(page, toResponses(page.getContent()));
    }

    @Transactional(readOnly = true)
    public List<FineResponse> forMember(Long memberId) {
        return toResponses(fineRepository.findByBorrowing_Member_IdOrderByCreatedAtDesc(memberId));
    }

    @Transactional(readOnly = true)
    public BigDecimal outstandingForMember(Long memberId) {
        return fineRepository.outstandingForMember(memberId);
    }

    /** Records a full or partial payment. A fine can never be overpaid. */
    @Transactional
    public PaymentResponse recordPayment(Long fineId, PaymentRequest req) {
        Fine fine = fineRepository.findByIdForUpdate(fineId).orElseThrow(() -> ResourceNotFoundException.of("Fine", fineId));
        if (fine.getStatus() == FineStatus.PAID || fine.getStatus() == FineStatus.WAIVED) {
            throw new BusinessRuleException("INVALID_PAYMENT", "This fine is already " + fine.getStatus().name().toLowerCase());
        }
        BigDecimal outstanding = fine.getOutstanding();
        BigDecimal amount = req.amount().setScale(2, java.math.RoundingMode.HALF_UP);
        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessRuleException("INVALID_PAYMENT", "Amount " + Formats.money(amount)
                    + " is more than the outstanding " + Formats.money(outstanding));
        }
        boolean needsReference = req.paymentMethod() == PaymentMethod.UPI || req.paymentMethod() == PaymentMethod.CARD;
        if (needsReference && !TextUtils.hasText(req.referenceNumber())) {
            throw new BusinessRuleException("INVALID_PAYMENT", "Enter the transaction reference number for "
                    + req.paymentMethod() + " payments");
        }

        fine.setPaidAmount(fine.getPaidAmount().add(amount));
        BigDecimal balance = fine.getOutstanding();
        fine.setStatus(balance.signum() == 0 ? FineStatus.PAID : FineStatus.PARTIALLY_PAID);

        FinePayment p = new FinePayment();
        p.setFine(fine);
        p.setAmount(amount);
        p.setBalanceAfter(balance);
        p.setPaymentMethod(req.paymentMethod());
        p.setReferenceNumber(TextUtils.clean(req.referenceNumber()));
        p.setRemarks(TextUtils.clean(req.remarks()));
        p.setPaymentDate(LocalDateTime.now());
        p.setPaymentStatus(balance.signum() == 0 ? PaymentStatus.PAID : PaymentStatus.PARTIAL);
        p.setRecordedBy(currentUser.currentUserRef());
        paymentRepository.save(p);

        auditService.log("RECORD_PAYMENT", "Fine", fine.getId(), Formats.money(amount) + " by " + req.paymentMethod()
                + " from " + fine.getBorrowing().getMember().getMemberCode() + ", balance " + Formats.money(balance));
        return CirculationMapper.toPaymentResponse(p);
    }

    /** Admin only: forgive whatever is still owed. The waiver is kept for the record. */
    @Transactional
    public FineResponse waive(Long fineId, WaiveRequest req) {
        Fine fine = fineRepository.findByIdForUpdate(fineId).orElseThrow(() -> ResourceNotFoundException.of("Fine", fineId));
        BigDecimal outstanding = fine.getOutstanding();
        if (outstanding.signum() == 0) {
            throw new BusinessRuleException("INVALID_PAYMENT", "Nothing is left to waive on this fine");
        }
        fine.setWaivedAmount(fine.getWaivedAmount().add(outstanding));
        fine.setStatus(FineStatus.WAIVED);
        auditService.log("WAIVE_FINE", "Fine", fine.getId(), "Waived " + Formats.money(outstanding) + ": " + req.reason());
        return toResponses(List.of(fine)).get(0);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> payments(String q, PaymentMethod method, LocalDate from, LocalDate to, Pageable pageable) {
        Page<FinePayment> page = paymentRepository.findAll(SearchSpecs.payments(q, method, from, to), pageable);
        return PageResponse.of(page, page.getContent().stream().map(CirculationMapper::toPaymentResponse).toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> paymentsForMember(Long memberId) {
        return paymentRepository.findByFine_Borrowing_Member_IdOrderByPaymentDateDesc(memberId).stream()
                .map(CirculationMapper::toPaymentResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> paymentsForFine(Long fineId) {
        return paymentRepository.findByFine_IdOrderByPaymentDateAsc(fineId).stream()
                .map(CirculationMapper::toPaymentResponse).toList();
    }

    List<FineResponse> toResponses(List<Fine> fines) {
        if (fines.isEmpty()) return List.of();
        Map<Long, ReturnRecord> returns = returnRepository
                .findByBorrowing_IdIn(fines.stream().map(f -> f.getBorrowing().getId()).toList()).stream()
                .collect(Collectors.toMap(r -> r.getBorrowing().getId(), Function.identity()));
        return fines.stream().map(f -> CirculationMapper.toFineResponse(f, returns.get(f.getBorrowing().getId()))).toList();
    }
}
