package com.college.library.entity;

import com.college.library.service.FinePolicy;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Single-row table (id = 1) holding every configurable library rule. */
@Entity
@Table(name = "library_settings")
public class LibrarySettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "fine_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal finePerDay;

    @Column(name = "max_fine_per_book", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxFinePerBook;

    @Column(name = "grace_period_days", nullable = false)
    private int gracePeriodDays;

    @Column(name = "max_books_per_member", nullable = false)
    private int maxBooksPerMember;

    @Column(name = "loan_period_days", nullable = false)
    private int loanPeriodDays;

    @Column(name = "max_renewals", nullable = false)
    private int maxRenewals;

    @Column(name = "renewal_period_days", nullable = false)
    private int renewalPeriodDays;

    @Column(name = "allow_renewal_when_overdue", nullable = false)
    private boolean allowRenewalWhenOverdue;

    @Column(name = "block_issue_on_unpaid_fines", nullable = false)
    private boolean blockIssueOnUnpaidFines;

    /** Issuing is blocked when outstanding fines are greater than this amount. */
    @Column(name = "fine_block_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal fineBlockThreshold;

    @Column(name = "reservation_hold_days", nullable = false)
    private int reservationHoldDays;

    @Column(name = "due_reminder_days", nullable = false)
    private int dueReminderDays;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    /** The three numbers the fine calculator needs. */
    public FinePolicy toFinePolicy() {
        return new FinePolicy(finePerDay, maxFinePerBook, gracePeriodDays);
    }

    // ----- getters & setters -----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getFinePerDay() { return finePerDay; }
    public void setFinePerDay(BigDecimal finePerDay) { this.finePerDay = finePerDay; }
    public BigDecimal getMaxFinePerBook() { return maxFinePerBook; }
    public void setMaxFinePerBook(BigDecimal maxFinePerBook) { this.maxFinePerBook = maxFinePerBook; }
    public int getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(int gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }
    public int getMaxBooksPerMember() { return maxBooksPerMember; }
    public void setMaxBooksPerMember(int maxBooksPerMember) { this.maxBooksPerMember = maxBooksPerMember; }
    public int getLoanPeriodDays() { return loanPeriodDays; }
    public void setLoanPeriodDays(int loanPeriodDays) { this.loanPeriodDays = loanPeriodDays; }
    public int getMaxRenewals() { return maxRenewals; }
    public void setMaxRenewals(int maxRenewals) { this.maxRenewals = maxRenewals; }
    public int getRenewalPeriodDays() { return renewalPeriodDays; }
    public void setRenewalPeriodDays(int renewalPeriodDays) { this.renewalPeriodDays = renewalPeriodDays; }
    public boolean isAllowRenewalWhenOverdue() { return allowRenewalWhenOverdue; }
    public void setAllowRenewalWhenOverdue(boolean allowRenewalWhenOverdue) { this.allowRenewalWhenOverdue = allowRenewalWhenOverdue; }
    public boolean isBlockIssueOnUnpaidFines() { return blockIssueOnUnpaidFines; }
    public void setBlockIssueOnUnpaidFines(boolean blockIssueOnUnpaidFines) { this.blockIssueOnUnpaidFines = blockIssueOnUnpaidFines; }
    public BigDecimal getFineBlockThreshold() { return fineBlockThreshold; }
    public void setFineBlockThreshold(BigDecimal fineBlockThreshold) { this.fineBlockThreshold = fineBlockThreshold; }
    public int getReservationHoldDays() { return reservationHoldDays; }
    public void setReservationHoldDays(int reservationHoldDays) { this.reservationHoldDays = reservationHoldDays; }
    public int getDueReminderDays() { return dueReminderDays; }
    public void setDueReminderDays(int dueReminderDays) { this.dueReminderDays = dueReminderDays; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
