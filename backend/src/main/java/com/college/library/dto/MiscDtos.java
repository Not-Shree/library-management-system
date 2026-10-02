package com.college.library.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Notifications, settings, audit logs and reports. */
public final class MiscDtos {

    private MiscDtos() {
    }

    public record NotificationResponse(Long id, String type, String title, String message, boolean read,
                                       LocalDateTime createdAt) {
    }

    public record SettingsDto(
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal finePerDay,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal maxFinePerBook,
            @NotNull @Min(0) @Max(60) Integer gracePeriodDays,
            @NotNull @Min(1) @Max(20) Integer maxBooksPerMember,
            @NotNull @Min(1) @Max(180) Integer loanPeriodDays,
            @NotNull @Min(0) @Max(10) Integer maxRenewals,
            @NotNull @Min(1) @Max(90) Integer renewalPeriodDays,
            @NotNull Boolean allowRenewalWhenOverdue,
            @NotNull Boolean blockIssueOnUnpaidFines,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal fineBlockThreshold,
            @NotNull @Min(1) @Max(30) Integer reservationHoldDays,
            @NotNull @Min(0) @Max(14) Integer dueReminderDays,
            String updatedBy,
            LocalDateTime updatedAt) {
    }

    public record AuditLogResponse(Long id, String username, String action, String entityType, Long entityId,
                                   String details, LocalDateTime createdAt) {
    }

    /** A generic table so every report can be shown in the UI and exported to CSV the same way. */
    public record ReportTable(String title, String description, List<String> columns, List<List<Object>> rows) {
    }
}
