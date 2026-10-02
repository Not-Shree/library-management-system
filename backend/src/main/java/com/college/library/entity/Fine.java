package com.college.library.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** A late-return fine. At most one per borrowing; paid in one or more {@link FinePayment}s. */
@Entity
@Table(name = "fines")
public class Fine extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrowing_id", nullable = false, unique = true)
    private Borrowing borrowing;

    @Column(name = "overdue_days", nullable = false)
    private int overdueDays;

    /** Rate copied from settings when the fine was created. */
    @Column(name = "fine_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal finePerDay;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "waived_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal waivedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FineStatus status = FineStatus.PENDING;

    /** Amount the member still owes. */
    public BigDecimal getOutstanding() {
        return amount.subtract(paidAmount).subtract(waivedAmount);
    }

    // ----- getters & setters -----
    public Borrowing getBorrowing() { return borrowing; }
    public void setBorrowing(Borrowing borrowing) { this.borrowing = borrowing; }
    public int getOverdueDays() { return overdueDays; }
    public void setOverdueDays(int overdueDays) { this.overdueDays = overdueDays; }
    public BigDecimal getFinePerDay() { return finePerDay; }
    public void setFinePerDay(BigDecimal finePerDay) { this.finePerDay = finePerDay; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getWaivedAmount() { return waivedAmount; }
    public void setWaivedAmount(BigDecimal waivedAmount) { this.waivedAmount = waivedAmount; }
    public FineStatus getStatus() { return status; }
    public void setStatus(FineStatus status) { this.status = status; }
}
