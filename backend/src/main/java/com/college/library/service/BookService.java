package com.college.library.service;

import com.college.library.dto.BookDtos.*;
import com.college.library.dto.PageResponse;
import com.college.library.entity.*;
import com.college.library.exception.BusinessRuleException;
import com.college.library.exception.DuplicateResourceException;
import com.college.library.exception.ResourceNotFoundException;
import com.college.library.mapper.BookMapper;
import com.college.library.repository.*;
import com.college.library.util.TextUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Catalogue titles and their physical copies. */
@Service
public class BookService {

    /** Copies that no longer count as part of the collection. */
    private static final List<CopyStatus> NOT_COUNTED = List.of(CopyStatus.WITHDRAWN, CopyStatus.LOST);
    /** Statuses a librarian may set by hand. BORROWED and RESERVED only come from issue/reservation. */
    private static final Set<CopyStatus> MANUAL_STATUSES =
            EnumSet.of(CopyStatus.AVAILABLE, CopyStatus.DAMAGED, CopyStatus.LOST, CopyStatus.WITHDRAWN);

    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final CategoryRepository categoryRepository;
    private final BorrowingRepository borrowingRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final AuditService auditService;

    public BookService(BookRepository bookRepository, BookCopyRepository copyRepository,
                       AuthorRepository authorRepository, PublisherRepository publisherRepository,
                       CategoryRepository categoryRepository, BorrowingRepository borrowingRepository,
                       ReservationRepository reservationRepository, ReservationService reservationService,
                       AuditService auditService) {
        this.bookRepository = bookRepository;
        this.copyRepository = copyRepository;
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
        this.categoryRepository = categoryRepository;
        this.borrowingRepository = borrowingRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.auditService = auditService;
    }

    // ---------------------------------------------------------------- search & read
    @Transactional(readOnly = true)
    public PageResponse<BookResponse> search(BookSearchCriteria criteria, Pageable pageable) {
        Page<Book> page = bookRepository.findAll(SearchSpecs.books(criteria), pageable);
        return PageResponse.of(page, toResponses(page.getContent()));
    }

    @Transactional(readOnly = true)
    public BookResponse get(Long id) {
        return toResponses(List.of(findBook(id))).get(0);
    }

    @Transactional(readOnly = true)
    public List<String> languages() {
        return bookRepository.findDistinctLanguages();
    }

    // ---------------------------------------------------------------- create / update / delete
    @Transactional
    public BookResponse create(BookRequest req) {
        String isbn = normaliseIsbn(req.isbn());
        if (bookRepository.existsByIsbnAndDeletedFalse(isbn)) {
            throw new DuplicateResourceException("DUPLICATE_ISBN", "A book with ISBN " + isbn + " already exists");
        }
        Book book = new Book();
        apply(book, req, isbn);
        bookRepository.save(book);

        int copies = req.initialCopies() != null ? req.initialCopies() : 0;
        if (copies > 0) {
            createCopies(book, null, copies, LocalDate.now(), null);
        }
        auditService.log("CREATE_BOOK", "Book", book.getId(), "\"" + book.getTitle() + "\" (" + isbn + ") with " + copies + " copies");
        return get(book.getId());
    }

    @Transactional
    public BookResponse update(Long id, BookRequest req) {
        Book book = findBook(id);
        String isbn = normaliseIsbn(req.isbn());
        if (bookRepository.existsByIsbnAndDeletedFalseAndIdNot(isbn, id)) {
            throw new DuplicateResourceException("DUPLICATE_ISBN", "Another book already uses ISBN " + isbn);
        }
        apply(book, req, isbn);
        auditService.log("UPDATE_BOOK", "Book", id, "\"" + book.getTitle() + "\"");
        return get(id);
    }

    /**
     * Soft delete: the book disappears from the catalogue but every past borrowing, return
     * and fine keeps pointing at it, so history is never destroyed.
     */
    @Transactional
    public void delete(Long id) {
        Book book = findBook(id);
        if (borrowingRepository.existsByBookCopy_Book_IdAndStatus(id, BorrowingStatus.BORROWED)) {
            throw new BusinessRuleException(HttpStatus.CONFLICT, "BOOK_IN_USE",
                    "Some copies of this book are still borrowed. Wait for them to be returned first.");
        }
        for (BookCopy copy : copyRepository.findByBook_IdOrderByCopyCodeAsc(id)) {
            copy.setStatus(CopyStatus.WITHDRAWN);
        }
        reservationService.cancelAllForBook(id);
        book.setDeleted(true);
        auditService.log("DELETE_BOOK", "Book", id, "\"" + book.getTitle() + "\" removed from catalogue (history kept)");
    }

    private void apply(Book book, BookRequest req, String isbn) {
        book.setIsbn(isbn);
        book.setTitle(req.title().trim());
        book.setSubtitle(TextUtils.clean(req.subtitle()));
        book.setAuthor(authorRepository.findById(req.authorId())
                .orElseThrow(() -> ResourceNotFoundException.of("Author", req.authorId())));
        book.setPublisher(req.publisherId() == null ? null : publisherRepository.findById(req.publisherId())
                .orElseThrow(() -> ResourceNotFoundException.of("Publisher", req.publisherId())));
        book.setCategory(categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Category", req.categoryId())));
        book.setLanguage(req.language().trim());
        book.setEdition(TextUtils.clean(req.edition()));
        book.setPublicationYear(req.publicationYear());
        book.setDescription(TextUtils.clean(req.description()));
        book.setShelfNumber(TextUtils.clean(req.shelfNumber()));
        book.setCoverImageUrl(TextUtils.clean(req.coverImageUrl()));
    }

    private static String normaliseIsbn(String isbn) {
        return isbn.replace("-", "").trim().toUpperCase();
    }

    private Book findBook(Long id) {
        return bookRepository.findByIdAndDeletedFalse(id).orElseThrow(() -> ResourceNotFoundException.of("Book", id));
    }

    // ---------------------------------------------------------------- copies
    @Transactional(readOnly = true)
    public List<BookCopyResponse> copies(Long bookId) {
        findBook(bookId);
        List<BookCopy> copies = copyRepository.findByBook_IdOrderByCopyCodeAsc(bookId);
        Map<Long, Borrowing> loans = borrowingRepository
                .findByBookCopy_IdInAndStatus(copies.stream().map(BookCopy::getId).toList(), BorrowingStatus.BORROWED)
                .stream().collect(Collectors.toMap(b -> b.getBookCopy().getId(), Function.identity()));
        return copies.stream().map(c -> BookMapper.toCopyResponse(c, loans.get(c.getId()))).toList();
    }

    /** Library-wide copy lookup (by copy code, title or ISBN), used by the Book Copies page. */
    @Transactional(readOnly = true)
    public PageResponse<BookCopyResponse> searchCopies(String q, CopyStatus status, Pageable pageable) {
        Page<BookCopy> page = copyRepository.findAll(SearchSpecs.copies(q, status), pageable);
        Map<Long, Borrowing> loans = borrowingRepository
                .findByBookCopy_IdInAndStatus(page.getContent().stream().map(BookCopy::getId).toList(), BorrowingStatus.BORROWED)
                .stream().collect(Collectors.toMap(b -> b.getBookCopy().getId(), Function.identity()));
        return PageResponse.of(page, page.getContent().stream().map(c -> BookMapper.toCopyResponse(c, loans.get(c.getId()))).toList());
    }

    @Transactional
    public List<BookCopyResponse> addCopies(Long bookId, AddCopiesRequest req) {
        Book book = findBook(bookId);
        String code = TextUtils.clean(req.copyCode());
        int quantity = req.quantity() != null ? req.quantity() : 1;
        if (code != null && quantity != 1) {
            throw new BusinessRuleException("INVALID_COPY", "Give a copy code for a single copy, or leave it empty to add several");
        }
        List<BookCopy> created = createCopies(book, code, quantity,
                req.acquiredDate() != null ? req.acquiredDate() : LocalDate.now(), TextUtils.clean(req.notes()));
        auditService.log("ADD_COPIES", "Book", bookId, "Added " + created.stream().map(BookCopy::getCopyCode).toList());
        return created.stream().map(c -> BookMapper.toCopyResponse(c, null)).toList();
    }

    private List<BookCopy> createCopies(Book book, String code, int quantity, LocalDate acquired, String notes) {
        List<BookCopy> created = new ArrayList<>();
        long next = copyRepository.countByBook_Id(book.getId()) + 1;
        for (int i = 0; i < quantity; i++) {
            String copyCode = code;
            if (copyCode == null) {
                // Generated codes look like LIB-0021-001 (book id + running number)
                do {
                    copyCode = String.format("LIB-%04d-%03d", book.getId(), next++);
                } while (copyRepository.existsByCopyCodeIgnoreCase(copyCode));
            } else if (copyRepository.existsByCopyCodeIgnoreCase(copyCode)) {
                throw new DuplicateResourceException("DUPLICATE_COPY_CODE", "Copy code " + copyCode + " is already used");
            }
            BookCopy copy = new BookCopy();
            copy.setBook(book);
            copy.setCopyCode(copyCode.toUpperCase());
            copy.setAcquiredDate(acquired);
            copy.setNotes(notes);
            copy.setStatus(CopyStatus.AVAILABLE);
            copyRepository.save(copy);
            // A new copy goes to the first person waiting, if anyone is.
            reservationService.offerCopyToQueue(copy);
            created.add(copy);
        }
        return created;
    }

    /** Mark a copy damaged, lost, withdrawn, or back to available after repair. */
    @Transactional
    public BookCopyResponse updateCopy(Long copyId, UpdateCopyRequest req) {
        BookCopy copy = copyRepository.findByIdForUpdate(copyId).orElseThrow(() -> ResourceNotFoundException.of("Book copy", copyId));
        if (copy.getBook().isDeleted()) {
            throw new BusinessRuleException("INVALID_COPY", "The book of this copy was removed from the catalogue");
        }
        if (copy.getStatus() == CopyStatus.BORROWED) {
            throw new BusinessRuleException(HttpStatus.CONFLICT, "COPY_IN_USE", "This copy is borrowed. Process the return instead.");
        }
        if (copy.getStatus() == CopyStatus.RESERVED) {
            throw new BusinessRuleException(HttpStatus.CONFLICT, "COPY_IN_USE",
                    "This copy is held for a reservation. Cancel the reservation first.");
        }
        if (!MANUAL_STATUSES.contains(req.status())) {
            throw new BusinessRuleException("INVALID_COPY", "Status " + req.status() + " is set automatically by the system");
        }
        CopyStatus old = copy.getStatus();
        copy.setNotes(TextUtils.clean(req.notes()));
        if (req.status() == CopyStatus.AVAILABLE && old != CopyStatus.AVAILABLE) {
            reservationService.offerCopyToQueue(copy);
        } else {
            copy.setStatus(req.status());
        }
        auditService.log("UPDATE_COPY", "BookCopy", copyId, copy.getCopyCode() + ": " + old + " -> " + copy.getStatus());
        return BookMapper.toCopyResponse(copy, null);
    }

    // ---------------------------------------------------------------- mapping
    /** Copy counts and reservation counts for a page of books, in two queries. */
    private List<BookResponse> toResponses(List<Book> books) {
        if (books.isEmpty()) return List.of();
        List<Long> ids = books.stream().map(Book::getId).toList();
        Map<Long, long[]> counts = new HashMap<>();
        for (Object[] row : copyRepository.countCopiesForBooks(ids, NOT_COUNTED)) {
            counts.put((Long) row[0], new long[]{((Number) row[1]).longValue(), row[2] == null ? 0 : ((Number) row[2]).longValue()});
        }
        List<BookResponse> out = new ArrayList<>();
        for (Book b : books) {
            long[] c = counts.getOrDefault(b.getId(), new long[]{0, 0});
            long reservations = reservationRepository.countByBook_IdAndStatusIn(b.getId(), ReservationService.ACTIVE);
            out.add(BookMapper.toResponse(b, c[0], c[1], reservations));
        }
        return out;
    }
}
