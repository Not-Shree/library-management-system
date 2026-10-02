package com.library.lms.repository;

import com.library.lms.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);

    @Query("SELECT b FROM Book b WHERE " +
           "(:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:isbn IS NULL OR b.isbn = :isbn) AND " +
           "(:categoryId IS NULL OR b.category.categoryId = :categoryId) AND " +
           "(:authorId IS NULL OR b.author.authorId = :authorId) AND " +
           "(:language IS NULL OR b.language = :language) AND " +
           "(:availableOnly = false OR b.availableCopies > 0)")
    Page<Book> search(String title, String isbn, Long categoryId, Long authorId,
                       String language, boolean availableOnly, Pageable pageable);
}
