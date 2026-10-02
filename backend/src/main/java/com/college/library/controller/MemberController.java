package com.college.library.controller;

import com.college.library.dto.CirculationDtos.BorrowingResponse;
import com.college.library.dto.FineDtos.FineResponse;
import com.college.library.dto.FineDtos.PaymentResponse;
import com.college.library.dto.MemberDtos.*;
import com.college.library.dto.PageResponse;
import com.college.library.dto.ReservationDtos.ReservationResponse;
import com.college.library.entity.MemberStatus;
import com.college.library.service.BorrowingService;
import com.college.library.service.FineService;
import com.college.library.service.MemberService;
import com.college.library.service.ReservationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
@Tag(name = "Members")
public class MemberController {

    private static final Map<String, String> SORTS = Map.of("name", "fullName", "code", "memberCode", "joined", "membershipDate");

    private final MemberService memberService;
    private final BorrowingService borrowingService;
    private final FineService fineService;
    private final ReservationService reservationService;

    public MemberController(MemberService memberService, BorrowingService borrowingService, FineService fineService,
                            ReservationService reservationService) {
        this.memberService = memberService;
        this.borrowingService = borrowingService;
        this.fineService = fineService;
        this.reservationService = reservationService;
    }

    @GetMapping
    public PageResponse<MemberResponse> search(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) MemberStatus status,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(defaultValue = "name") String sort,
                                               @RequestParam(defaultValue = "asc") String dir) {
        return memberService.search(q, status, Paging.of(page, size, SORTS.getOrDefault(sort, "fullName"), dir));
    }

    @GetMapping("/{id}")
    public MemberResponse get(@PathVariable Long id) {
        return memberService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberRequest req) {
        return memberService.create(req);
    }

    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberRequest req) {
        return memberService.update(id, req);
    }

    @PatchMapping("/{id}/status")
    public MemberResponse setStatus(@PathVariable Long id, @Valid @RequestBody MemberStatusRequest req) {
        return memberService.setStatus(id, req.status());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        memberService.delete(id);
    }

    @PostMapping("/{id}/login")
    public MemberResponse createLogin(@PathVariable Long id, @Valid @RequestBody CreateLoginRequest req) {
        return memberService.createLogin(id, req);
    }

    @GetMapping("/{id}/borrowings")
    public PageResponse<BorrowingResponse> history(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return borrowingService.search(null, null, false, id, Paging.of(page, size, "issueDate", "desc"));
    }

    @GetMapping("/{id}/fines")
    public List<FineResponse> fines(@PathVariable Long id) {
        return fineService.forMember(id);
    }

    @GetMapping("/{id}/payments")
    public List<PaymentResponse> payments(@PathVariable Long id) {
        return fineService.paymentsForMember(id);
    }

    @GetMapping("/{id}/reservations")
    public List<ReservationResponse> reservations(@PathVariable Long id) {
        return reservationService.forMember(id);
    }
}
