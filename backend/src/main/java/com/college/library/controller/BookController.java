package com.college.library.controller;

import com.college.library.dto.BookDtos.*;
import com.college.library.dto.PageResponse;
import com.college.library.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@Tag(name = "Books")
public class BookController {

    private static final Map<String, String> SORTS = Map.of(
            "title", "title", "year", "publicationYear", "author", "author.name", "newest", "createdAt");

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @Operation(summary = "Search the catalogue (public)",
            description = "q searches title, subtitle, ISBN, author, publisher and category. sort: title | year | author | newest")
    public PageResponse<BookResponse> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) Long categoryId,
                                             @RequestParam(required = false) Long authorId,
                                             @RequestParam(required = false) Long publisherId,
                                             @RequestParam(required = false) String language,
                                             @RequestParam(required = false) Integer yearFrom,
                                             @RequestParam(required = false) Integer yearTo,
                                             @RequestParam(required = false) Boolean available,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "12") int size,
                                             @RequestParam(defaultValue = "title") String sort,
                                             @RequestParam(defaultValue = "asc") String dir) {
        var criteria = new BookSearchCriteria(q, categoryId, authorId, publisherId, language, yearFrom, yearTo, available);
        return bookService.search(criteria, Paging.of(page, size, SORTS.getOrDefault(sort, "title"), dir));
    }

    @GetMapping("/{id}")
    public BookResponse get(@PathVariable Long id) {
        return bookService.get(id);
    }

    @GetMapping("/meta/languages")
    public List<String> languages() {
        return bookService.languages();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest req) {
        return bookService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest req) {
        return bookService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a book from the catalogue (soft delete, history is kept)")
    public void delete(@PathVariable Long id) {
        bookService.delete(id);
    }

    // ---------------------------------------------------------------- copies
    @GetMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public List<BookCopyResponse> copies(@PathVariable Long id) {
        return bookService.copies(id);
    }

    @PostMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @ResponseStatus(HttpStatus.CREATED)
    public List<BookCopyResponse> addCopies(@PathVariable Long id, @Valid @RequestBody AddCopiesRequest req) {
        return bookService.addCopies(id, req);
    }

    @PutMapping("/copies/{copyId}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Mark a copy available, damaged, lost or withdrawn")
    public BookCopyResponse updateCopy(@PathVariable Long copyId, @Valid @RequestBody UpdateCopyRequest req) {
        return bookService.updateCopy(copyId, req);
    }
}
