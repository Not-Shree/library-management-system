package com.college.library.service;

import com.college.library.dto.CirculationDtos.BorrowingResponse;
import com.college.library.dto.MiscDtos.ReportTable;
import com.college.library.entity.*;
import com.college.library.exception.BusinessRuleException;
import com.college.library.mapper.CirculationMapper;
import com.college.library.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * All reports return the same {@link ReportTable} shape (title, columns, rows),
 * so the UI shows them with one component and CSV export is shared.
 * PDF export is not implemented in this version.
 */
@Service
public class ReportService {

    public static final Map<String, String> REPORT_TYPES = new LinkedHashMap<>();

    static {
        REPORT_TYPES.put("borrowed", "Currently borrowed books");
        REPORT_TYPES.put("overdue", "Overdue books");
        REPORT_TYPES.put("returned", "Returned books");
        REPORT_TYPES.put("fine-collection", "Fine collection");
        REPORT_TYPES.put("outstanding-fines", "Outstanding fines");
        REPORT_TYPES.put("most-borrowed", "Most borrowed books");
        REPORT_TYPES.put("active-members", "Most active members");
        REPORT_TYPES.put("books-by-category", "Books by category");
        REPORT_TYPES.put("inventory", "Book inventory");
        REPORT_TYPES.put("reservations", "Reservations");
    }

    private final BorrowingRepository borrowingRepository;
    private final ReturnRecordRepository returnRepository;
    private final FineRepository fineRepository;
    private final FinePaymentRepository paymentRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final ReservationRepository reservationRepository;
    private final BorrowingService borrowingService;
    private final FineService fineService;

    public ReportService(BorrowingRepository borrowingRepository, ReturnRecordRepository returnRepository,
                         FineRepository fineRepository, FinePaymentRepository paymentRepository,
                         BookRepository bookRepository, BookCopyRepository copyRepository,
                         ReservationRepository reservationRepository, BorrowingService borrowingService,
                         FineService fineService) {
        this.borrowingRepository = borrowingRepository;
        this.returnRepository = returnRepository;
        this.fineRepository = fineRepository;
        this.paymentRepository = paymentRepository;
        this.bookRepository = bookRepository;
        this.copyRepository = copyRepository;
        this.reservationRepository = reservationRepository;
        this.borrowingService = borrowingService;
        this.fineService = fineService;
    }

    /** from/to are optional; they default to the last 30 days for date-based reports. */
    @Transactional(readOnly = true)
    public ReportTable generate(String type, LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(30);
        if (start.isAfter(end)) {
            throw new BusinessRuleException("INVALID_DATE_RANGE", "The start date must be before the end date");
        }
        String range = " (" + start + " to " + end + ")";
        return switch (type) {
            case "borrowed" -> borrowingTable(REPORT_TYPES.get(type), "All books that are out right now",
                    borrowingRepository.findByStatusOrderByDueDateAsc(BorrowingStatus.BORROWED));
            case "overdue" -> borrowingTable(REPORT_TYPES.get(type), "Borrowed books past their due date, with the fine so far",
                    borrowingRepository.findByStatusAndDueDateBeforeOrderByDueDateAsc(BorrowingStatus.BORROWED, LocalDate.now()));
            case "returned" -> returned(start, end, range);
            case "fine-collection" -> fineCollection(start, end, range);
            case "outstanding-fines" -> outstandingFines();
            case "most-borrowed" -> new ReportTable(REPORT_TYPES.get(type), "Top 20 titles by number of loans" + range,
                    List.of("Title", "Times borrowed"),
                    rows(borrowingRepository.mostBorrowedBooks(start, end, PageRequest.of(0, 20))));
            case "active-members" -> new ReportTable(REPORT_TYPES.get(type), "Top 20 members by number of loans" + range,
                    List.of("Member ID", "Name", "Books borrowed"),
                    rows(borrowingRepository.mostActiveMembers(start, end, PageRequest.of(0, 20))));
            case "books-by-category" -> new ReportTable(REPORT_TYPES.get(type), "Titles in the catalogue per category",
                    List.of("Category", "Titles"),
                    bookRepository.countActiveBooksByCategory().stream().map(r -> List.<Object>of(r[1], r[2])).toList());
            case "inventory" -> inventory();
            case "reservations" -> reservations(start, end, range);
            default -> throw new BusinessRuleException("UNKNOWN_REPORT", "Unknown report \"" + type + "\". Available: " + REPORT_TYPES.keySet());
        };
    }

    private ReportTable borrowingTable(String title, String description, List<Borrowing> borrowings) {
        List<List<Object>> rows = new ArrayList<>();
        for (BorrowingResponse b : borrowingService.toResponses(borrowings)) {
            rows.add(Arrays.asList(b.memberCode(), b.memberName(), b.bookTitle(), b.copyCode(), b.issueDate(), b.dueDate(),
                    b.overdueDays(), b.estimatedFine()));
        }
        return new ReportTable(title, description,
                List.of("Member ID", "Member", "Title", "Copy", "Issued", "Due", "Days overdue", "Fine so far (₹)"), rows);
    }

    private ReportTable returned(LocalDate start, LocalDate end, String range) {
        List<List<Object>> rows = new ArrayList<>();
        for (ReturnRecord r : returnRepository.findByReturnDateBetweenOrderByReturnDateDesc(start, end)) {
            Borrowing b = r.getBorrowing();
            Fine fine = fineRepository.findByBorrowing_Id(b.getId()).orElse(null);
            rows.add(Arrays.asList(b.getMember().getMemberCode(), b.getMember().getFullName(), b.getBookCopy().getBook().getTitle(),
                    b.getBookCopy().getCopyCode(), b.getIssueDate(), b.getDueDate(), r.getReturnDate(), r.getOverdueDays(),
                    r.getBookCondition(), fine != null ? fine.getAmount() : null));
        }
        return new ReportTable(REPORT_TYPES.get("returned"), "Books returned" + range,
                List.of("Member ID", "Member", "Title", "Copy", "Issued", "Due", "Returned", "Days late", "Condition", "Fine (₹)"), rows);
    }

    private ReportTable fineCollection(LocalDate start, LocalDate end, String range) {
        List<List<Object>> rows = new ArrayList<>();
        for (FinePayment p : paymentRepository.findByPaymentDateBetweenOrderByPaymentDateDesc(start.atStartOfDay(), end.plusDays(1).atStartOfDay())) {
            var r = CirculationMapper.toPaymentResponse(p);
            rows.add(Arrays.asList(r.paymentDate().toLocalDate(), r.memberCode(), r.memberName(), r.bookTitle(), r.paymentMethod(),
                    r.referenceNumber(), r.paidAmount(), r.remainingAmount(), r.recordedBy()));
        }
        return new ReportTable(REPORT_TYPES.get("fine-collection"), "Payments recorded" + range,
                List.of("Date", "Member ID", "Member", "Title", "Method", "Reference", "Paid (₹)", "Balance (₹)", "Recorded by"), rows);
    }

    private ReportTable outstandingFines() {
        List<Fine> fines = fineRepository.findByStatusInOrderByCreatedAtAsc(List.of(FineStatus.PENDING, FineStatus.PARTIALLY_PAID));
        List<List<Object>> rows = fineService.toResponses(fines).stream().map(f -> Arrays.<Object>asList(
                f.memberCode(), f.memberName(), f.bookTitle(), f.returnDate(), f.overdueDays(), f.amount(),
                f.paidAmount(), f.outstandingAmount(), f.status())).toList();
        return new ReportTable(REPORT_TYPES.get("outstanding-fines"), "Fines not yet fully paid",
                List.of("Member ID", "Member", "Title", "Returned", "Days late", "Fine (₹)", "Paid (₹)", "Outstanding (₹)", "Status"), rows);
    }

    private ReportTable inventory() {
        Map<Long, Map<CopyStatus, Long>> counts = new HashMap<>();
        for (Object[] r : copyRepository.countByBookAndStatus()) {
            counts.computeIfAbsent((Long) r[0], k -> new EnumMap<>(CopyStatus.class)).put((CopyStatus) r[1], ((Number) r[2]).longValue());
        }
        List<List<Object>> rows = new ArrayList<>();
        for (Book b : bookRepository.findByDeletedFalseOrderByTitleAsc()) {
            Map<CopyStatus, Long> c = counts.getOrDefault(b.getId(), Map.of());
            long total = c.entrySet().stream().filter(e -> e.getKey() != CopyStatus.WITHDRAWN && e.getKey() != CopyStatus.LOST)
                    .mapToLong(Map.Entry::getValue).sum();
            rows.add(Arrays.asList(b.getIsbn(), b.getTitle(), b.getAuthor().getName(), b.getCategory().getName(), b.getShelfNumber(),
                    total, c.getOrDefault(CopyStatus.AVAILABLE, 0L), c.getOrDefault(CopyStatus.BORROWED, 0L),
                    c.getOrDefault(CopyStatus.RESERVED, 0L), c.getOrDefault(CopyStatus.DAMAGED, 0L), c.getOrDefault(CopyStatus.LOST, 0L)));
        }
        return new ReportTable(REPORT_TYPES.get("inventory"), "Copies per title by status",
                List.of("ISBN", "Title", "Author", "Category", "Shelf", "Total", "Available", "Borrowed", "On hold", "Damaged", "Lost"), rows);
    }

    private ReportTable reservations(LocalDate start, LocalDate end, String range) {
        List<List<Object>> rows = new ArrayList<>();
        for (Reservation r : reservationRepository.findByReservedAtBetweenOrderByReservedAtDesc(start.atStartOfDay(), end.plusDays(1).atStartOfDay())) {
            rows.add(Arrays.asList(r.getReservedAt().toLocalDate(), r.getMember().getMemberCode(), r.getMember().getFullName(),
                    r.getBook().getTitle(), r.getStatus(), r.getHeldCopy() != null ? r.getHeldCopy().getCopyCode() : null, r.getExpiryDate()));
        }
        return new ReportTable(REPORT_TYPES.get("reservations"), "Reservations placed" + range,
                List.of("Reserved on", "Member ID", "Member", "Title", "Status", "Held copy", "Collect by"), rows);
    }

    private static List<List<Object>> rows(List<Object[]> raw) {
        return raw.stream().map(r -> Arrays.asList(r)).toList();
    }
}
