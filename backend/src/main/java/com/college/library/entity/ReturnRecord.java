package com.college.library.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Details of a completed return (table "returns"). */
@Entity
@Table(name = "returns")
public class ReturnRecord extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrowing_id", nullable = false, unique = true)
    private Borrowing borrowing;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "overdue_days", nullable = false)
    private int overdueDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_condition", nullable = false, length = 20)
    private BookCondition bookCondition = BookCondition.GOOD;

    @Column(length = 255)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private User receivedBy;

    // ----- getters & setters -----
    public Borrowing getBorrowing() { return borrowing; }
    public void setBorrowing(Borrowing borrowing) { this.borrowing = borrowing; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public int getOverdueDays() { return overdueDays; }
    public void setOverdueDays(int overdueDays) { this.overdueDays = overdueDays; }
    public BookCondition getBookCondition() { return bookCondition; }
    public void setBookCondition(BookCondition bookCondition) { this.bookCondition = bookCondition; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public User getReceivedBy() { return receivedBy; }
    public void setReceivedBy(User receivedBy) { this.receivedBy = receivedBy; }
}
