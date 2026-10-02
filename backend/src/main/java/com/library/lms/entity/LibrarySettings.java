package com.library.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Single-row configuration table. The FineCalculator and BorrowingService
 * must always read from here instead of using hard-coded constants.
 */
@Entity
@Table(name = "library_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibrarySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Integer settingId;

    @Column(name = "fine_per_day", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal finePerDay = new BigDecimal("5.00");

    @Column(name = "max_fine_per_book", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal maxFinePerBook = new BigDecimal("500.00");

    @Column(name = "grace_period_days", nullable = false)
    @Builder.Default
    private Integer gracePeriodDays = 0;

    @Column(name = "max_books_allowed", nullable = false)
    @Builder.Default
    private Integer maxBooksAllowed = 3;

    @Column(name = "default_loan_period_days", nullable = false)
    @Builder.Default
    private Integer defaultLoanPeriodDays = 14;

    @Column(name = "max_renewal_count", nullable = false)
    @Builder.Default
    private Integer maxRenewalCount = 2;

    @Column(name = "renewal_period_days", nullable = false)
    @Builder.Default
    private Integer renewalPeriodDays = 7;

    @Column(name = "reservation_expiry_days", nullable = false)
    @Builder.Default
    private Integer reservationExpiryDays = 3;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
    }
}
