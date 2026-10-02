package com.college.library.controller;

import com.college.library.dto.BookDtos.BookCopyResponse;
import com.college.library.dto.PageResponse;
import com.college.library.entity.CopyStatus;
import com.college.library.service.BookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Search physical copies across the whole library (e.g. scan or type LIB-CC-002). */
@RestController
@RequestMapping("/api/copies")
@PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
@Tag(name = "Books")
public class CopyController {

    private final BookService bookService;

    public CopyController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public PageResponse<BookCopyResponse> search(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) CopyStatus status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "15") int size) {
        return bookService.searchCopies(q, status, Paging.of(page, size, "copyCode", "asc"));
    }
}
