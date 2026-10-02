package com.college.library.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** One physical copy of a book, e.g. LIB-CC-001. */
@Entity
@Table(name = "book_copies")
public class BookCopy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "copy_code", nullable = false, unique = true, length = 40)
    private String copyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CopyStatus status = CopyStatus.AVAILABLE;

    @Column(name = "acquired_date")
    private LocalDate acquiredDate;

    @Column(length = 255)
    private String notes;

    /** Optimistic lock: two librarians cannot update the same copy at the same moment. */
    @Version
    @Column(nullable = false)
    private Long version;

    // ----- getters & setters -----
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public String getCopyCode() { return copyCode; }
    public void setCopyCode(String copyCode) { this.copyCode = copyCode; }
    public CopyStatus getStatus() { return status; }
    public void setStatus(CopyStatus status) { this.status = status; }
    public LocalDate getAcquiredDate() { return acquiredDate; }
    public void setAcquiredDate(LocalDate acquiredDate) { this.acquiredDate = acquiredDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
