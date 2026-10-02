package com.college.library.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A place in the first-come-first-served queue for a book with no free copy. */
@Entity
@Table(name = "reservations")
public class Reservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.WAITING;

    /** The copy set aside for this member once the reservation becomes AVAILABLE. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_copy_id")
    private BookCopy heldCopy;

    @Column(name = "available_at")
    private LocalDateTime availableAt;

    /** Last day the member can collect the held copy. */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    public boolean isActive() {
        return status == ReservationStatus.WAITING || status == ReservationStatus.AVAILABLE;
    }

    // ----- getters & setters -----
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public LocalDateTime getReservedAt() { return reservedAt; }
    public void setReservedAt(LocalDateTime reservedAt) { this.reservedAt = reservedAt; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public BookCopy getHeldCopy() { return heldCopy; }
    public void setHeldCopy(BookCopy heldCopy) { this.heldCopy = heldCopy; }
    public LocalDateTime getAvailableAt() { return availableAt; }
    public void setAvailableAt(LocalDateTime availableAt) { this.availableAt = availableAt; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
}
