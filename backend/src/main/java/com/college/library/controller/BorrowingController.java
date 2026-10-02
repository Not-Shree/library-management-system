package com.college.library.controller;

import com.college.library.dto.CirculationDtos.*;
import com.college.library.dto.PageResponse;
import com.college.library.entity.BorrowingStatus;
import com.college.library.service.BorrowingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/borrowings")
@Tag(name = "Borrowings (issue / return / renew)")
public class BorrowingController {

    private static final Map<String, String> SORTS = Map.of("issued", "issueDate", "due", "dueDate");

    private final BorrowingService borrowingService;

    public BorrowingController(BorrowingService borrowingService) {
        this.borrowingService = borrowingService;
    }

    @PostMapping("/issue")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Issue a copy to a member (checks limit, availability and unpaid fines)")
    public BorrowingResponse issue(@Valid @RequestBody IssueRequest req) {
        return borrowingService.issue(req);
    }

    @GetMapping("/{id}/return-preview")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Calculate the fine for a return without saving anything")
    public ReturnPreview previewReturn(@PathVariable Long id,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate) {
        return borrowingService.previewReturn(id, returnDate);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Return a book; the fine is calculated automatically")
    public ReturnResponse returnBook(@PathVariable Long id, @Valid @RequestBody(required = false) ReturnRequest req) {
        return borrowingService.returnBook(id, req != null ? req : new ReturnRequest(null, null, null));
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "Renew a loan (staff, or the member who borrowed it)")
    public BorrowingResponse renew(@PathVariable Long id) {
        return borrowingService.renew(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public PageResponse<BorrowingResponse> search(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) BorrowingStatus status,
                                                  @RequestParam(defaultValue = "false") boolean overdue,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(defaultValue = "issued") String sort,
                                                  @RequestParam(defaultValue = "desc") String dir) {
        return borrowingService.search(q, status, overdue, null, Paging.of(page, size, SORTS.getOrDefault(sort, "issueDate"), dir));
    }

    @GetMapping("/{id}")
    public BorrowingResponse get(@PathVariable Long id) {
        return borrowingService.get(id);
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public PageResponse<BorrowingResponse> forMember(@PathVariable Long memberId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return borrowingService.search(null, null, false, memberId, Paging.of(page, size, "issueDate", "desc"));
    }
}
