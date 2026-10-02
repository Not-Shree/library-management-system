package com.college.library.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ReservationDtos {

    private ReservationDtos() {
    }

    /** memberId is required for staff; members always reserve for themselves. */
    public record ReservationRequest(@NotNull Long bookId, Long memberId) {
    }

    public record ReservationResponse(Long id, Long memberId, String memberCode, String memberName,
                                      Long bookId, String bookTitle, LocalDateTime reservedAt, String status,
                                      Integer queuePosition, String heldCopyCode, LocalDateTime availableAt,
                                      LocalDate expiryDate) {
    }
}
