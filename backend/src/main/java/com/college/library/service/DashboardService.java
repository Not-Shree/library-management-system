package com.college.library.service;

import com.college.library.dto.CirculationDtos.BorrowingResponse;
import com.college.library.dto.DashboardDtos.*;
import com.college.library.entity.*;
import com.college.library.repository.*;
import com.college.library.security.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final List<CopyStatus> NOT_COUNTED = List.of(CopyStatus.WITHDRAWN, CopyStatus.LOST);

    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final BorrowingRepository borrowingRepository;
    private final ReturnRecordRepository returnRepository;
    private final MemberRepository memberRepository;
    private final FineRepository fineRepository;
    private final FinePaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationRepository notificationRepository;
    private final BorrowingService borrowingService;
    private final SettingsService settingsService;
    private final CurrentUserService currentUser;

    public DashboardService(BookRepository bookRepository, BookCopyRepository copyRepository,
                            BorrowingRepository borrowingRepository, ReturnRecordRepository returnRepository,
                            MemberRepository memberRepository, FineRepository fineRepository,
                            FinePaymentRepository paymentRepository, ReservationRepository reservationRepository,
                            NotificationRepository notificationRepository, BorrowingService borrowingService,
                            SettingsService settingsService, CurrentUserService currentUser) {
        this.bookRepository = bookRepository;
        this.copyRepository = copyRepository;
        this.borrowingRepository = borrowingRepository;
        this.returnRepository = returnRepository;
        this.memberRepository = memberRepository;
        this.fineRepository = fineRepository;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.notificationRepository = notificationRepository;
        this.borrowingService = borrowingService;
        this.settingsService = settingsService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public AdminDashboard admin() {
        LocalDate today = LocalDate.now();
        LocalDate yearAgo = today.minusYears(1);
        List<NameValue> mostBorrowed = borrowingRepository.mostBorrowedBooks(yearAgo, today, PageRequest.of(0, 5)).stream()
                .map(r -> new NameValue((String) r[0], (Number) r[1])).toList();
        List<NameValue> mostActive = borrowingRepository.mostActiveMembers(yearAgo, today, PageRequest.of(0, 5)).stream()
                .map(r -> new NameValue((String) r[1], (Number) r[2])).toList();
        List<NameValue> byCategory = bookRepository.countActiveBooksByCategory().stream()
                .map(r -> new NameValue((String) r[1], (Number) r[2])).toList();

        return new AdminDashboard(
                bookRepository.countByDeletedFalse(),
                copyRepository.countByStatusNotIn(NOT_COUNTED),
                copyRepository.countByStatus(CopyStatus.AVAILABLE),
                borrowingRepository.countByStatus(BorrowingStatus.BORROWED),
                borrowingRepository.countByStatusAndDueDateBefore(BorrowingStatus.BORROWED, today),
                memberRepository.count(),
                memberRepository.countByStatus(MemberStatus.ACTIVE),
                fineRepository.totalOutstanding(),
                paymentRepository.totalCollected(),
                reservationRepository.countByStatusIn(ReservationService.ACTIVE),
                monthly(6), mostBorrowed, mostActive, byCategory);
    }

    @Transactional(readOnly = true)
    public LibrarianDashboard librarian() {
        LocalDate today = LocalDate.now();
        int reminderDays = Math.max(1, settingsService.current().getDueReminderDays());
        List<Borrowing> overdue = borrowingRepository.findByStatusAndDueDateBeforeOrderByDueDateAsc(BorrowingStatus.BORROWED, today);
        List<BorrowingResponse> topOverdue = borrowingService.toResponses(overdue.stream().limit(8).toList());
        return new LibrarianDashboard(
                borrowingRepository.countByIssueDate(today),
                returnRepository.countByReturnDate(today),
                overdue.size(),
                borrowingRepository.countByStatusAndDueDateBetween(BorrowingStatus.BORROWED, today, today.plusDays(reminderDays)),
                reservationRepository.countByStatus(ReservationStatus.WAITING),
                reservationRepository.countByStatus(ReservationStatus.AVAILABLE),
                fineRepository.totalOutstanding(),
                paymentRepository.collectedBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay()),
                monthly(6), topOverdue);
    }

    @Transactional(readOnly = true)
    public MemberDashboard member() {
        Member m = currentUser.requireCurrentMember();
        LibrarySettings s = settingsService.current();
        List<BorrowingResponse> current = borrowingService.currentForMember(m.getId());
        BigDecimal accruing = current.stream().map(BorrowingResponse::estimatedFine).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long overdue = current.stream().filter(b -> "OVERDUE".equals(b.displayStatus())).count();
        return new MemberDashboard(m.getFullName(), m.getMemberCode(), BorrowingService.borrowLimit(m, s),
                current.size(), overdue, fineRepository.outstandingForMember(m.getId()), accruing,
                reservationRepository.countByMember_IdAndStatusIn(m.getId(), ReservationService.ACTIVE),
                reservationRepository.countByMember_IdAndStatus(m.getId(), ReservationStatus.AVAILABLE),
                notificationRepository.countByMember_IdAndReadFalse(m.getId()), current);
    }

    /** Issued / returned / fines collected for the last N months, with empty months filled with zero. */
    private List<MonthlyPoint> monthly(int months) {
        YearMonth thisMonth = YearMonth.now();
        LocalDate from = thisMonth.minusMonths(months - 1L).atDay(1);
        Map<String, Long> issued = toLongMap(borrowingRepository.issuedPerMonth(from));
        Map<String, Long> returned = toLongMap(returnRepository.returnedPerMonth(from));
        Map<String, BigDecimal> collected = new HashMap<>();
        for (Object[] r : paymentRepository.collectedPerMonth(from)) {
            collected.put((String) r[0], new BigDecimal(r[1].toString()));
        }
        List<MonthlyPoint> points = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = thisMonth.minusMonths(i);
            String key = ym.toString();  // "2026-09"
            points.add(new MonthlyPoint(ym.format(MONTH_LABEL), issued.getOrDefault(key, 0L),
                    returned.getOrDefault(key, 0L), collected.getOrDefault(key, BigDecimal.ZERO)));
        }
        return points;
    }

    private static Map<String, Long> toLongMap(List<Object[]> rows) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] r : rows) map.put((String) r[0], ((Number) r[1]).longValue());
        return map;
    }
}
