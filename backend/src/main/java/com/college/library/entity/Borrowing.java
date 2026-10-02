package com.college.library.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** A loan of one physical copy to one member. Kept forever as borrowing history. */
@Entity
@Table(name = "borrowings")
public class Borrowing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_copy_id", nullable = false)
    private BookCopy bookCopy;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "renewal_count", nullable = false)
    private int renewalCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowingStatus status = BorrowingStatus.BORROWED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    @Column(name = "due_reminder_sent", nullable = false)
    private boolean dueReminderSent = false;

    @Column(name = "overdue_notice_sent", nullable = false)
    private boolean overdueNoticeSent = false;

    /** Overdue is not stored — it is derived from the due date. */
    public boolean isOverdue(LocalDate today) {
        return status == BorrowingStatus.BORROWED && today.isAfter(dueDate);
    }

    // ----- getters & setters -----
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public BookCopy getBookCopy() { return bookCopy; }
    public void setBookCopy(BookCopy bookCopy) { this.bookCopy = bookCopy; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public int getRenewalCount() { return renewalCount; }
    public void setRenewalCount(int renewalCount) { this.renewalCount = renewalCount; }
    public BorrowingStatus getStatus() { return status; }
    public void setStatus(BorrowingStatus status) { this.status = status; }
    public User getIssuedBy() { return issuedBy; }
    public void setIssuedBy(User issuedBy) { this.issuedBy = issuedBy; }
    public boolean isDueReminderSent() { return dueReminderSent; }
    public void setDueReminderSent(boolean dueReminderSent) { this.dueReminderSent = dueReminderSent; }
    public boolean isOverdueNoticeSent() { return overdueNoticeSent; }
    public void setOverdueNoticeSent(boolean overdueNoticeSent) { this.overdueNoticeSent = overdueNoticeSent; }
}
