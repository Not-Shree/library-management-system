package com.college.library.repository;

import com.college.library.entity.Borrowing;
import com.college.library.entity.BorrowingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BorrowingRepository extends JpaRepository<Borrowing, Long>, JpaSpecificationExecutor<Borrowing> {

    long countByMember_IdAndStatus(Long memberId, BorrowingStatus status);

    long countByMember_IdAndStatusAndDueDateBefore(Long memberId, BorrowingStatus status, LocalDate date);

    boolean existsByMember_Id(Long memberId);

    boolean existsByMember_IdAndBookCopy_Book_IdAndStatus(Long memberId, Long bookId, BorrowingStatus status);

    long countByStatus(BorrowingStatus status);

    long countByStatusAndDueDateBefore(BorrowingStatus status, LocalDate date);

    long countByStatusAndDueDateBetween(BorrowingStatus status, LocalDate from, LocalDate to);

    long countByIssueDate(LocalDate issueDate);

    boolean existsByBookCopy_Book_IdAndStatus(Long bookId, BorrowingStatus status);

    Optional<Borrowing> findFirstByBookCopy_IdAndStatus(Long bookCopyId, BorrowingStatus status);

    List<Borrowing> findByBookCopy_IdInAndStatus(Collection<Long> copyIds, BorrowingStatus status);

    List<Borrowing> findByMember_IdAndStatusOrderByDueDateAsc(Long memberId, BorrowingStatus status);

    List<Borrowing> findByStatusOrderByDueDateAsc(BorrowingStatus status);

    List<Borrowing> findByStatusAndDueDateBeforeOrderByDueDateAsc(BorrowingStatus status, LocalDate date);

    List<Borrowing> findByStatusAndDueReminderSentFalseAndDueDateBetween(BorrowingStatus status, LocalDate from, LocalDate to);

    List<Borrowing> findByStatusAndOverdueNoticeSentFalseAndDueDateBefore(BorrowingStatus status, LocalDate date);

    /** Rows of [memberId, activeCount] for a page of members. */
    @Query("select b.member.id, count(b) from Borrowing b where b.status = :status and b.member.id in :memberIds group by b.member.id")
    List<Object[]> countByMemberIds(@Param("memberIds") Collection<Long> memberIds, @Param("status") BorrowingStatus status);

    /** Rows of [bookTitle, timesBorrowed]. */
    @Query("""
            select bk.title, count(b) from Borrowing b join b.bookCopy c join c.book bk
            where b.issueDate between :from and :to
            group by bk.id, bk.title order by count(b) desc""")
    List<Object[]> mostBorrowedBooks(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable limit);

    /** Rows of [memberCode, memberName, timesBorrowed]. */
    @Query("""
            select m.memberCode, m.fullName, count(b) from Borrowing b join b.member m
            where b.issueDate between :from and :to
            group by m.id, m.memberCode, m.fullName order by count(b) desc""")
    List<Object[]> mostActiveMembers(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable limit);

    /** Rows of ["YYYY-MM", count]. */
    @Query(value = """
            select to_char(date_trunc('month', issue_date), 'YYYY-MM') as month, count(*)
            from borrowings where issue_date >= :from group by 1 order by 1""", nativeQuery = true)
    List<Object[]> issuedPerMonth(@Param("from") LocalDate from);
}
