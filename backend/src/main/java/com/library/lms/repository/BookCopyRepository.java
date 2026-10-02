package com.library.lms.repository;

import com.library.lms.entity.BookCopy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    List<BookCopy> findByBook_BookId(Long bookId);
    Optional<BookCopy> findByCopyCode(String copyCode);
    boolean existsByCopyCode(String copyCode);
    Optional<BookCopy> findFirstByBook_BookIdAndStatus(Long bookId, String status);
}
