package com.college.library.repository;

import com.college.library.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByIdAndDeletedFalse(Long id);

    boolean existsByIsbnAndDeletedFalse(String isbn);

    boolean existsByIsbnAndDeletedFalseAndIdNot(String isbn, Long id);

    long countByDeletedFalse();

    boolean existsByAuthor_Id(Long authorId);

    boolean existsByPublisher_Id(Long publisherId);

    boolean existsByCategory_Id(Long categoryId);

    List<Book> findByDeletedFalseOrderByTitleAsc();

    @Query("select distinct b.language from Book b where b.deleted = false order by b.language")
    List<String> findDistinctLanguages();

    /** Rows of [authorId, bookCount]. */
    @Query("select b.author.id, count(b) from Book b where b.deleted = false group by b.author.id")
    List<Object[]> countActiveBooksByAuthor();

    @Query("select b.publisher.id, count(b) from Book b where b.deleted = false and b.publisher is not null group by b.publisher.id")
    List<Object[]> countActiveBooksByPublisher();

    /** Rows of [categoryId, categoryName, bookCount] for the "books by category" chart. */
    @Query("select c.id, c.name, count(b) from Book b join b.category c where b.deleted = false group by c.id, c.name order by count(b) desc")
    List<Object[]> countActiveBooksByCategory();
}
