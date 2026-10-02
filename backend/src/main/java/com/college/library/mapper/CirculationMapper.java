package com.college.library.mapper;

import com.college.library.dto.CirculationDtos.BorrowingResponse;
import com.college.library.dto.FineDtos.FineResponse;
import com.college.library.dto.FineDtos.PaymentResponse;
import com.college.library.dto.ReservationDtos.ReservationResponse;
import com.college.library.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Maps borrowings, fines, payments and reservations to API responses. */
public final class CirculationMapper {

    private CirculationMapper() {
    }

    /**
     * @param ret           return record, or null if the book is still out
     * @param fine          fine, or null if none
     * @param estimatedFine fine the member would pay if the overdue book came back today (may be null)
     */
    public static BorrowingResponse toBorrowingResponse(Borrowing b, ReturnRecord ret, Fine fine,
                                                        LocalDate today, BigDecimal estimatedFine) {
        Member m = b.getMember();
        BookCopy copy = b.getBookCopy();
        Book book = copy.getBook();
        boolean overdue = b.isOverdue(today);
        String displayStatus = b.getStatus() == BorrowingStatus.RETURNED ? "RETURNED" : (overdue ? "OVERDUE" : "BORROWED");
        int overdueDays;
        if (ret != null) {
            overdueDays = ret.getOverdueDays();
        } else {
            overdueDays = overdue ? (int) java.time.temporal.ChronoUnit.DAYS.between(b.getDueDate(), today) : 0;
        }
        return new BorrowingResponse(b.getId(), m.getId(), m.getMemberCode(), m.getFullName(),
                book.getId(), book.getTitle(), book.getIsbn(), copy.getId(), copy.getCopyCode(),
                b.getIssueDate(), b.getDueDate(), ret != null ? ret.getReturnDate() : null,
                b.getStatus().name(), displayStatus, b.getRenewalCount(), overdueDays,
                fine != null ? fine.getAmount() : null, fine != null ? fine.getStatus().name() : null,
                ret == null ? estimatedFine : null,
                b.getIssuedBy() != null ? b.getIssuedBy().getUsername() : null,
                ret != null ? ret.getBookCondition().name() : null);
    }

    public static FineResponse toFineResponse(Fine f, ReturnRecord ret) {
        Borrowing b = f.getBorrowing();
        Member m = b.getMember();
        return new FineResponse(f.getId(), b.getId(), m.getId(), m.getMemberCode(), m.getFullName(),
                b.getBookCopy().getBook().getTitle(), b.getBookCopy().getCopyCode(), b.getDueDate(),
                ret != null ? ret.getReturnDate() : null, f.getOverdueDays(), f.getFinePerDay(), f.getAmount(),
                f.getPaidAmount(), f.getWaivedAmount(), f.getOutstanding(), f.getStatus().name(), f.getCreatedAt());
    }

    public static PaymentResponse toPaymentResponse(FinePayment p) {
        Fine f = p.getFine();
        Borrowing b = f.getBorrowing();
        Member m = b.getMember();
        return new PaymentResponse(p.getId(), f.getId(), b.getId(), m.getId(), m.getMemberCode(), m.getFullName(),
                b.getBookCopy().getBook().getTitle(), f.getAmount(), p.getAmount(), p.getBalanceAfter(),
                p.getPaymentDate(), p.getPaymentMethod().name(), p.getReferenceNumber(), p.getPaymentStatus().name(),
                p.getRecordedBy() != null ? p.getRecordedBy().getUsername() : null, p.getRemarks());
    }

    public static ReservationResponse toReservationResponse(Reservation r, Integer queuePosition) {
        Member m = r.getMember();
        return new ReservationResponse(r.getId(), m.getId(), m.getMemberCode(), m.getFullName(),
                r.getBook().getId(), r.getBook().getTitle(), r.getReservedAt(), r.getStatus().name(), queuePosition,
                r.getHeldCopy() != null ? r.getHeldCopy().getCopyCode() : null, r.getAvailableAt(), r.getExpiryDate());
    }
}
