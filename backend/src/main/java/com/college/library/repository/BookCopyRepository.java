package com.college.library.repository;

import com.college.library.entity.BookCopy;
import com.college.library.entity.CopyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long>, JpaSpecificationExecutor<BookCopy> {

    List<BookCopy> findByBook_IdOrderByCopyCodeAsc(Long bookId);

    List<BookCopy> findByBook_IdAndStatus(Long bookId, CopyStatus status);

    Optional<BookCopy> findByCopyCodeIgnoreCase(String copyCode);

    boolean existsByCopyCodeIgnoreCase(String copyCode);

    boolean existsByBook_IdAndStatus(Long bookId, CopyStatus status);

    long countByBook_Id(Long bookId);

    long countByBook_IdAndStatusNotIn(Long bookId, Collection<CopyStatus> statuses);

    long countByStatus(CopyStatus status);

    long countByStatusNotIn(Collection<CopyStatus> statuses);

    /**
     * Locks the copy row until the transaction ends (SELECT ... FOR UPDATE), so two librarians
     * can never issue the same copy at the same moment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopy c where c.id = :id")
    Optional<BookCopy> findByIdForUpdate(@Param("id") Long id);

    /** Rows of [bookId, totalCopies, availableCopies] for a page of books (avoids N+1 queries). */
    @Query("""
            select c.book.id, count(c),
                   sum(case when c.status = com.college.library.entity.CopyStatus.AVAILABLE then 1 else 0 end)
            from BookCopy c
            where c.book.id in :bookIds and c.status not in :excluded
            group by c.book.id""")
    List<Object[]> countCopiesForBooks(@Param("bookIds") Collection<Long> bookIds,
                                       @Param("excluded") Collection<CopyStatus> excluded);

    /** Inventory report rows: [bookId, status, count]. */
    @Query("select c.book.id, c.status, count(c) from BookCopy c group by c.book.id, c.status")
    List<Object[]> countByBookAndStatus();
}
