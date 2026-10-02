package com.college.library.dto;

import com.college.library.entity.BookCondition;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Issue, return and renew. */
public final class CirculationDtos {

    private CirculationDtos() {
    }

    /** Give either bookCopyId (picked from a list) or copyCode (typed / scanned). */
    public record IssueRequest(@NotNull(message = "Member is required") Long memberId, Long bookCopyId,
                               @Size(max = 40) String copyCode) {
    }

    /** returnDate is optional; it defaults to today and cannot be in the future. */
    public record ReturnRequest(LocalDate returnDate, BookCondition bookCondition, @Size(max = 255) String remarks) {
    }

    /**
     * One row of borrowing history. displayStatus is BORROWED, OVERDUE or RETURNED.
     * fineAmount is the real fine for a returned book; estimatedFine is what an overdue book
     * would cost if it were returned today.
     */
    public record BorrowingResponse(Long id, Long memberId, String memberCode, String memberName,
                                    Long bookId, String bookTitle, String isbn, Long bookCopyId, String copyCode,
                                    LocalDate issueDate, LocalDate dueDate, LocalDate returnDate,
                                    String status, String displayStatus, int renewalCount, int overdueDays,
                                    BigDecimal fineAmount, String fineStatus, BigDecimal estimatedFine,
                                    String issuedBy, String bookCondition) {
    }

    /** Everything the return screen shows before the librarian confirms. */
    public record ReturnPreview(BorrowingResponse borrowing, LocalDate returnDate, int overdueDays, int gracePeriodDays,
                                int chargeableDays, BigDecimal finePerDay, BigDecimal maxFinePerBook,
                                BigDecimal fineAmount, boolean capped, BigDecimal memberOutstandingFine) {
    }

    public record ReturnResponse(BorrowingResponse borrowing, FineDtos.FineResponse fine,
                                 String reservationNotice) {
    }
}
