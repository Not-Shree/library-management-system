package com.college.library.service;

import com.college.library.dto.CirculationDtos.*;
import com.college.library.dto.PageResponse;
import com.college.library.entity.*;
import com.college.library.exception.BusinessRuleException;
import com.college.library.exception.ResourceNotFoundException;
import com.college.library.mapper.CirculationMapper;
import com.college.library.repository.*;
import com.college.library.security.AppUserPrincipal;
import com.college.library.security.CurrentUserService;
import com.college.library.util.TextUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Issue, return and renew. Every library rule is checked here on the server,
 * so the rules hold even if someone calls the API directly (e.g. from Postman).
 */
@Service
public class BorrowingService {

    private final BorrowingRepository borrowingRepository;
    private final BookCopyRepository copyRepository;
    private final MemberRepository memberRepository;
    private final ReturnRecordRepository returnRepository;
    private final FineRepository fineRepository;
    private final ReservationRepository reservationRepository;
    private final SettingsService settingsService;
    private final FineCalculator fineCalculator;
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public BorrowingService(BorrowingRepository borrowingRepository, BookCopyRepository copyRepository,
                            MemberRepository memberRepository, ReturnRecordRepository returnRepository,
                            FineRepository fineRepository, ReservationRepository reservationRepository,
                            SettingsService settingsService, FineCalculator fineCalculator,
                            ReservationService reservationService, NotificationService notificationService,
                            AuditService auditService, CurrentUserService currentUser) {
        this.borrowingRepository = borrowingRepository;
        this.copyRepository = copyRepository;
        this.memberRepository = memberRepository;
        this.returnRepository = returnRepository;
        this.fineRepository = fineRepository;
        this.reservationRepository = reservationRepository;
        this.settingsService = settingsService;
        this.fineCalculator = fineCalculator;
        this.reservationService = reservationService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    // =====================================================================
    // ISSUE
    // =====================================================================
    @Transactional
    public BorrowingResponse issue(IssueRequest req) {
        LibrarySettings settings = settingsService.current();

        // 1. Member must exist and be active
        Member member = memberRepository.findById(req.memberId())
                .orElseThrow(() -> ResourceNotFoundException.of("Member", req.memberId()));
        if (!member.isActive()) {
            throw new BusinessRuleException("MEMBER_INACTIVE", member.getFullName() + "'s membership is inactive");
        }

        // 2. Lock the copy row (SELECT ... FOR UPDATE) so nobody else can issue it at the same time
        BookCopy copy = lockCopy(req);
        Book book = copy.getBook();
        if (book.isDeleted()) {
            throw new BusinessRuleException(HttpStatus.CONFLICT, "BOOK_UNAVAILABLE", "This book has been removed from the catalogue");
        }

        // 3. Only AVAILABLE copies can be issued (or a RESERVED copy, to the member it is held for)
        if (copy.getStatus() == CopyStatus.RESERVED) {
            Reservation hold = reservationRepository.findFirstByHeldCopy_IdAndStatus(copy.getId(), ReservationStatus.AVAILABLE)
                    .orElse(null);
            if (hold == null || !hold.getMember().getId().equals(member.getId())) {
                throw new BusinessRuleException(HttpStatus.CONFLICT, "BOOK_UNAVAILABLE",
                        "Copy " + copy.getCopyCode() + " is being held for another member's reservation");
            }
        } else if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new BusinessRuleException(HttpStatus.CONFLICT, "BOOK_UNAVAILABLE",
                    "Copy " + copy.getCopyCode() + " is " + copy.getStatus().name().toLowerCase() + " and cannot be issued");
        }

        // 4. Borrowing limit
        long current = borrowingRepository.countByMember_IdAndStatus(member.getId(), BorrowingStatus.BORROWED);
        int limit = borrowLimit(member, settings);
        if (current >= limit) {
            throw new BusinessRuleException("BORROW_LIMIT_EXCEEDED",
                    member.getFullName() + " already has " + current + " of " + limit + " allowed books");
        }

        // 5. One copy of the same title at a time
        if (borrowingRepository.existsByMember_IdAndBookCopy_Book_IdAndStatus(member.getId(), book.getId(), BorrowingStatus.BORROWED)) {
            throw new BusinessRuleException("ALREADY_BORROWED", member.getFullName() + " already has a copy of \"" + book.getTitle() + "\"");
        }

        // 6. Unpaid fines (only when enabled in settings)
        if (settings.isBlockIssueOnUnpaidFines()) {
            BigDecimal owed = fineRepository.outstandingForMember(member.getId());
            if (owed.compareTo(settings.getFineBlockThreshold()) > 0) {
                throw new BusinessRuleException("UNPAID_FINES", member.getFullName() + " owes " + Formats.money(owed)
                        + " in fines (limit " + Formats.money(settings.getFineBlockThreshold()) + "). Collect payment first.");
            }
        }

        // 7-10. Create the borrowing; dates come from the loan period setting
        LocalDate today = LocalDate.now();
        Borrowing b = new Borrowing();
        b.setMember(member);
        b.setBookCopy(copy);
        b.setIssueDate(today);
        b.setDueDate(today.plusDays(settings.getLoanPeriodDays()));
        b.setStatus(BorrowingStatus.BORROWED);
        b.setIssuedBy(currentUser.currentUserRef());
        copy.setStatus(CopyStatus.BORROWED);   // available copies drop automatically (they are counted, not stored)
        borrowingRepository.save(b);

        reservationService.markFulfilledOnIssue(member, copy);

        notificationService.notify(member, NotificationType.BOOK_ISSUED, "Book issued",
                "\"" + book.getTitle() + "\" (" + copy.getCopyCode() + ") was issued to you. Due on " + Formats.date(b.getDueDate()) + ".");
        auditService.log("ISSUE_BOOK", "Borrowing", b.getId(),
                "Issued " + copy.getCopyCode() + " to " + member.getMemberCode() + ", due " + b.getDueDate());
        return toResponse(b);
    }

    private BookCopy lockCopy(IssueRequest req) {
        Long copyId = req.bookCopyId();
        if (copyId == null) {
            if (!TextUtils.hasText(req.copyCode())) {
                throw new BusinessRuleException("COPY_REQUIRED", "Choose a book copy or enter its copy code");
            }
            copyId = copyRepository.findByCopyCodeIgnoreCase(req.copyCode().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("No copy with code " + req.copyCode().trim()))
                    .getId();
        }
        Long id = copyId;
        return copyRepository.findByIdForUpdate(id).orElseThrow(() -> ResourceNotFoundException.of("Book copy", id));
    }

    static int borrowLimit(Member m, LibrarySettings s) {
        return m.getMaxBooksAllowed() != null ? m.getMaxBooksAllowed() : s.getMaxBooksPerMember();
    }

    // =====================================================================
    // RETURN
    // =====================================================================

    /** Shows the fine before the librarian confirms the return. Nothing is saved. */
    @Transactional(readOnly = true)
    public ReturnPreview previewReturn(Long borrowingId, LocalDate returnDate) {
        Borrowing b = findActive(borrowingId);
        LocalDate date = validReturnDate(b, returnDate);
        LibrarySettings s = settingsService.current();
        FineResult fine = fineCalculator.calculate(b.getDueDate(), date, s.toFinePolicy());
        return new ReturnPreview(toResponse(b), date, fine.overdueDays(), s.getGracePeriodDays(), fine.chargeableDays(),
                s.getFinePerDay(), s.getMaxFinePerBook(), fine.amount(), fine.capped(),
                fineRepository.outstandingForMember(b.getMember().getId()));
    }

    @Transactional
    public ReturnResponse returnBook(Long borrowingId, ReturnRequest req) {
        Borrowing b = findActive(borrowingId);
        LocalDate returnDate = validReturnDate(b, req.returnDate());
        BookCondition condition = req.bookCondition() != null ? req.bookCondition() : BookCondition.GOOD;
        LibrarySettings settings = settingsService.current();

        // Automatic fine calculation, using the rules from library_settings
        FineResult result = fineCalculator.calculate(b.getDueDate(), returnDate, settings.toFinePolicy());

        ReturnRecord ret = new ReturnRecord();
        ret.setBorrowing(b);
        ret.setReturnDate(returnDate);
        ret.setOverdueDays(result.overdueDays());
        ret.setBookCondition(condition);
        ret.setRemarks(TextUtils.clean(req.remarks()));
        ret.setReceivedBy(currentUser.currentUserRef());
        returnRepository.save(ret);

        b.setStatus(BorrowingStatus.RETURNED);

        // Put the copy back: to the reservation queue / shelf if in good condition
        BookCopy copy = b.getBookCopy();
        String reservationNotice = null;
        switch (condition) {
            case GOOD -> {
                Optional<Reservation> hold = reservationService.offerCopyToQueue(copy);
                if (hold.isPresent()) {
                    Member next = hold.get().getMember();
                    reservationNotice = "Keep this copy aside: it is now held for " + next.getFullName()
                            + " (" + next.getMemberCode() + ") until " + Formats.date(hold.get().getExpiryDate()) + ".";
                }
            }
            case DAMAGED -> copy.setStatus(CopyStatus.DAMAGED);
            case LOST -> copy.setStatus(CopyStatus.LOST);
        }

        Fine fine = null;
        if (result.hasFine()) {
            fine = new Fine();
            fine.setBorrowing(b);
            fine.setOverdueDays(result.overdueDays());
            fine.setFinePerDay(settings.getFinePerDay());
            fine.setAmount(result.amount());
            fine.setStatus(FineStatus.PENDING);
            fineRepository.save(fine);
            notificationService.notify(b.getMember(), NotificationType.FINE_GENERATED, "Fine generated",
                    "A fine of " + Formats.money(result.amount()) + " was added for \"" + copy.getBook().getTitle()
                            + "\" (" + result.overdueDays() + " days late" + (result.capped() ? ", maximum fine applied" : "") + ").");
        }

        notificationService.notify(b.getMember(), NotificationType.BOOK_RETURNED, "Book returned",
                "\"" + copy.getBook().getTitle() + "\" was returned on " + Formats.date(returnDate)
                        + (result.overdueDays() > 0 ? ", " + result.overdueDays() + " days late." : ", on time."));
        auditService.log("RETURN_BOOK", "Borrowing", b.getId(), "Returned " + copy.getCopyCode() + " (" + condition + ")"
                + (fine != null ? ", " + result.overdueDays() + " days late, fine " + Formats.money(fine.getAmount()) : ", no fine"));

        return new ReturnResponse(CirculationMapper.toBorrowingResponse(b, ret, fine, LocalDate.now(), null),
                fine != null ? CirculationMapper.toFineResponse(fine, ret) : null, reservationNotice);
    }

    private Borrowing findActive(Long id) {
        Borrowing b = borrowingRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Borrowing", id));
        if (b.getStatus() != BorrowingStatus.BORROWED) {
            throw new BusinessRuleException("INVALID_RETURN", "This book has already been returned");
        }
        return b;
    }

    private static LocalDate validReturnDate(Borrowing b, LocalDate requested) {
        LocalDate today = LocalDate.now();
        LocalDate date = requested != null ? requested : today;
        if (date.isAfter(today)) {
            throw new BusinessRuleException("INVALID_RETURN", "Return date cannot be in the future");
        }
        if (date.isBefore(b.getIssueDate())) {
            throw new BusinessRuleException("INVALID_RETURN", "Return date cannot be before the issue date (" + b.getIssueDate() + ")");
        }
        return date;
    }

    // =====================================================================
    // RENEW
    // =====================================================================
    @Transactional
    public BorrowingResponse renew(Long borrowingId) {
        Borrowing b = borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Borrowing", borrowingId));
        checkAccess(b);
        LibrarySettings s = settingsService.current();
        LocalDate today = LocalDate.now();

        if (b.getStatus() != BorrowingStatus.BORROWED) {
            throw new BusinessRuleException("INVALID_RENEWAL", "Only books that are currently borrowed can be renewed");
        }
        if (b.getRenewalCount() >= s.getMaxRenewals()) {
            throw new BusinessRuleException("INVALID_RENEWAL", "This book has already been renewed the maximum "
                    + s.getMaxRenewals() + " times");
        }
        if (b.isOverdue(today) && !s.isAllowRenewalWhenOverdue()) {
            throw new BusinessRuleException("INVALID_RENEWAL", "Overdue books cannot be renewed. Return the book and pay the fine.");
        }
        Long bookId = b.getBookCopy().getBook().getId();
        if (reservationRepository.existsByBook_IdAndStatusInAndMember_IdNot(bookId, ReservationService.ACTIVE, b.getMember().getId())) {
            throw new BusinessRuleException("INVALID_RENEWAL", "Another member has reserved this book, so it cannot be renewed");
        }

        // Extend from the due date (or from today if an overdue renewal is allowed)
        LocalDate base = b.getDueDate().isBefore(today) ? today : b.getDueDate();
        LocalDate oldDue = b.getDueDate();
        b.setDueDate(base.plusDays(s.getRenewalPeriodDays()));
        b.setRenewalCount(b.getRenewalCount() + 1);
        b.setDueReminderSent(false);
        b.setOverdueNoticeSent(false);

        notificationService.notify(b.getMember(), NotificationType.BOOK_RENEWED, "Book renewed",
                "\"" + b.getBookCopy().getBook().getTitle() + "\" is now due on " + Formats.date(b.getDueDate())
                        + " (renewal " + b.getRenewalCount() + " of " + s.getMaxRenewals() + ").");
        auditService.log("RENEW_BOOK", "Borrowing", b.getId(), "Due date " + oldDue + " -> " + b.getDueDate());
        return toResponse(b);
    }

    // =====================================================================
    // QUERIES
    // =====================================================================
    @Transactional(readOnly = true)
    public PageResponse<BorrowingResponse> search(String q, BorrowingStatus status, boolean overdueOnly, Long memberId,
                                                  Pageable pageable) {
        Page<Borrowing> page = borrowingRepository.findAll(
                SearchSpecs.borrowings(q, status, overdueOnly, memberId, LocalDate.now()), pageable);
        return PageResponse.of(page, toResponses(page.getContent()));
    }

    @Transactional(readOnly = true)
    public BorrowingResponse get(Long id) {
        Borrowing b = borrowingRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Borrowing", id));
        checkAccess(b);
        return toResponse(b);
    }

    @Transactional(readOnly = true)
    public List<BorrowingResponse> currentForMember(Long memberId) {
        return toResponses(borrowingRepository.findByMember_IdAndStatusOrderByDueDateAsc(memberId, BorrowingStatus.BORROWED));
    }

    /** Members may only see their own borrowings. */
    private void checkAccess(Borrowing b) {
        AppUserPrincipal p = currentUser.requirePrincipal();
        if (!p.isStaff()) {
            User owner = b.getMember().getUser();
            if (owner == null || !owner.getId().equals(p.getId())) {
                throw new BusinessRuleException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This borrowing belongs to another member");
            }
        }
    }

    BorrowingResponse toResponse(Borrowing b) {
        return toResponses(List.of(b)).get(0);
    }

    /** Maps a list in a few queries: returns and fines are loaded in one batch each. */
    public List<BorrowingResponse> toResponses(List<Borrowing> borrowings) {
        if (borrowings.isEmpty()) return List.of();
        List<Long> ids = borrowings.stream().map(Borrowing::getId).toList();
        Map<Long, ReturnRecord> returns = returnRepository.findByBorrowing_IdIn(ids).stream()
                .collect(Collectors.toMap(r -> r.getBorrowing().getId(), Function.identity()));
        Map<Long, Fine> fines = fineRepository.findByBorrowing_IdIn(ids).stream()
                .collect(Collectors.toMap(f -> f.getBorrowing().getId(), Function.identity()));
        LocalDate today = LocalDate.now();
        FinePolicy policy = settingsService.current().toFinePolicy();
        List<BorrowingResponse> out = new ArrayList<>(borrowings.size());
        for (Borrowing b : borrowings) {
            BigDecimal estimate = b.isOverdue(today) ? fineCalculator.calculate(b.getDueDate(), today, policy).amount() : null;
            out.add(CirculationMapper.toBorrowingResponse(b, returns.get(b.getId()), fines.get(b.getId()), today, estimate));
        }
        return out;
    }
}
