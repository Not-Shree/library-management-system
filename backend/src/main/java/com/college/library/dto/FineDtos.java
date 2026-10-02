package com.college.library.dto;

import com.college.library.entity.PaymentMethod;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class FineDtos {

    private FineDtos() {
    }

    public record FineResponse(Long id, Long borrowingId, Long memberId, String memberCode, String memberName,
                               String bookTitle, String copyCode, LocalDate dueDate, LocalDate returnDate,
                               int overdueDays, BigDecimal finePerDay, BigDecimal amount, BigDecimal paidAmount,
                               BigDecimal waivedAmount, BigDecimal outstandingAmount, String status,
                               LocalDateTime createdAt) {
    }

    /** Mock/offline payment: the librarian records money that was received at the counter. */
    public record PaymentRequest(
            @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
            @Digits(integer = 8, fraction = 2) BigDecimal amount,
            @NotNull PaymentMethod paymentMethod,
            @Size(max = 60) String referenceNumber,
            @Size(max = 255) String remarks) {
    }

    public record WaiveRequest(@NotBlank @Size(max = 255) String reason) {
    }

    public record PaymentResponse(Long id, Long fineId, Long borrowingId, Long memberId, String memberCode,
                                  String memberName, String bookTitle, BigDecimal fineAmount, BigDecimal paidAmount,
                                  BigDecimal remainingAmount, LocalDateTime paymentDate, String paymentMethod,
                                  String referenceNumber, String paymentStatus, String recordedBy, String remarks) {
    }
}
