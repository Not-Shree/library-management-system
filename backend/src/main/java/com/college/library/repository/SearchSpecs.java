package com.college.library.repository;

import com.college.library.dto.BookDtos.BookSearchCriteria;
import com.college.library.entity.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.college.library.util.TextUtils.hasText;
import static com.college.library.util.TextUtils.likePattern;

/**
 * Dynamic WHERE clauses built with the JPA Criteria API. Every filter is optional,
 * and values are always bound as parameters, so there is no risk of SQL injection.
 */
public final class SearchSpecs {

    private static final char ESCAPE = '\\';

    private SearchSpecs() {
    }

    private static Predicate like(CriteriaBuilder cb, Expression<String> field, String pattern) {
        return cb.like(cb.lower(field), pattern, ESCAPE);
    }

    private static Predicate and(CriteriaBuilder cb, List<Predicate> predicates) {
        return cb.and(predicates.toArray(new Predicate[0]));
    }

    // ---------------------------------------------------------------- books
    public static Specification<Book> books(BookSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("deleted")));
            if (hasText(c.q())) {
                String p = likePattern(c.q());
                Join<Book, Author> author = root.join("author", JoinType.LEFT);
                Join<Book, Publisher> publisher = root.join("publisher", JoinType.LEFT);
                Join<Book, Category> category = root.join("category", JoinType.LEFT);
                ps.add(cb.or(
                        like(cb, root.get("title"), p),
                        like(cb, root.get("subtitle"), p),
                        like(cb, root.get("isbn"), p),
                        like(cb, author.get("name"), p),
                        like(cb, publisher.get("name"), p),
                        like(cb, category.get("name"), p)));
            }
            if (c.categoryId() != null) ps.add(cb.equal(root.get("category").get("id"), c.categoryId()));
            if (c.authorId() != null) ps.add(cb.equal(root.get("author").get("id"), c.authorId()));
            if (c.publisherId() != null) ps.add(cb.equal(root.get("publisher").get("id"), c.publisherId()));
            if (hasText(c.language())) ps.add(cb.equal(cb.lower(root.get("language")), c.language().trim().toLowerCase()));
            if (c.yearFrom() != null) ps.add(cb.greaterThanOrEqualTo(root.get("publicationYear"), c.yearFrom()));
            if (c.yearTo() != null) ps.add(cb.lessThanOrEqualTo(root.get("publicationYear"), c.yearTo()));
            if (c.available() != null) {
                // EXISTS (select 1 from book_copies where book_id = book.id and status = 'AVAILABLE')
                Subquery<Long> sq = query.subquery(Long.class);
                Root<BookCopy> copy = sq.from(BookCopy.class);
                sq.select(copy.get("id")).where(
                        cb.equal(copy.get("book"), root),
                        cb.equal(copy.get("status"), CopyStatus.AVAILABLE));
                ps.add(c.available() ? cb.exists(sq) : cb.not(cb.exists(sq)));
            }
            return and(cb, ps);
        };
    }

    // ---------------------------------------------------------------- copies
    public static Specification<BookCopy> copies(String q, CopyStatus status) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            Join<BookCopy, Book> book = root.join("book");
            ps.add(cb.isFalse(book.get("deleted")));
            if (hasText(q)) {
                String p = likePattern(q);
                ps.add(cb.or(like(cb, root.get("copyCode"), p), like(cb, book.get("title"), p), like(cb, book.get("isbn"), p)));
            }
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            return and(cb, ps);
        };
    }

    // ---------------------------------------------------------------- members
    public static Specification<Member> members(String q, MemberStatus status) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (hasText(q)) {
                String p = likePattern(q);
                ps.add(cb.or(like(cb, root.get("fullName"), p), like(cb, root.get("memberCode"), p),
                        like(cb, root.get("email"), p), like(cb, root.get("department"), p),
                        like(cb, root.get("phone"), p)));
            }
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            return and(cb, ps);
        };
    }

    // ---------------------------------------------------------------- borrowings
    public static Specification<Borrowing> borrowings(String q, BorrowingStatus status, boolean overdueOnly,
                                                      Long memberId, LocalDate today) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            Join<Borrowing, Member> member = root.join("member");
            Join<Borrowing, BookCopy> copy = root.join("bookCopy");
            if (hasText(q)) {
                String p = likePattern(q);
                Join<BookCopy, Book> book = copy.join("book");
                ps.add(cb.or(like(cb, member.get("fullName"), p), like(cb, member.get("memberCode"), p),
                        like(cb, copy.get("copyCode"), p), like(cb, book.get("title"), p),
                        like(cb, book.get("isbn"), p)));
            }
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (overdueOnly) {
                ps.add(cb.equal(root.get("status"), BorrowingStatus.BORROWED));
                ps.add(cb.lessThan(root.get("dueDate"), today));
            }
            if (memberId != null) ps.add(cb.equal(member.get("id"), memberId));
            return and(cb, ps);
        };
    }

    // ---------------------------------------------------------------- fines
    public static Specification<Fine> fines(String q, FineStatus status, Long memberId) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            Join<Fine, Borrowing> borrowing = root.join("borrowing");
            Join<Borrowing, Member> member = borrowing.join("member");
            if (hasText(q)) {
                String p = likePattern(q);
                Join<Borrowing, BookCopy> copy = borrowing.join("bookCopy");
                Join<BookCopy, Book> book = copy.join("book");
                ps.add(cb.or(like(cb, member.get("fullName"), p), like(cb, member.get("memberCode"), p),
                        like(cb, book.get("title"), p), like(cb, copy.get("copyCode"), p)));
            }
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (memberId != null) ps.add(cb.equal(member.get("id"), memberId));
            return and(cb, ps);
        };
    }

    // ---------------------------------------------------------------- payments
    public static Specification<FinePayment> payments(String q, PaymentMethod method, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (hasText(q)) {
                String p = likePattern(q);
                Join<FinePayment, Fine> fine = root.join("fine");
                Join<Fine, Borrowing> borrowing = fine.join("borrowing");
                Join<Borrowing, Member> member = borrowing.join("member");
                ps.add(cb.or(like(cb, member.get("fullName"), p), like(cb, member.get("memberCode"), p),
                        like(cb, root.get("referenceNumber"), p)));
            }
            if (method != null) ps.add(cb.equal(root.get("paymentMethod"), method));
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), from.atStartOfDay()));
            if (to != null) ps.add(cb.lessThan(root.get("paymentDate"), to.plusDays(1).atStartOfDay()));
            return and(cb, ps);
        };
    }

    // ---------------------------------------------------------------- reservations
    public static Specification<Reservation> reservations(String q, ReservationStatus status) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (hasText(q)) {
                String p = likePattern(q);
                Join<Reservation, Member> member = root.join("member");
                Join<Reservation, Book> book = root.join("book");
                ps.add(cb.or(like(cb, member.get("fullName"), p), like(cb, member.get("memberCode"), p),
                        like(cb, book.get("title"), p)));
            }
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            return and(cb, ps);
        };
    }

    // ---------------------------------------------------------------- audit logs
    public static Specification<AuditLog> auditLogs(String q, String entityType, LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (hasText(q)) {
                String p = likePattern(q);
                ps.add(cb.or(like(cb, root.get("username"), p), like(cb, root.get("action"), p),
                        like(cb, root.get("details"), p)));
            }
            if (hasText(entityType)) ps.add(cb.equal(root.get("entityType"), entityType));
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) ps.add(cb.lessThan(root.get("createdAt"), to));
            return and(cb, ps);
        };
    }
}
