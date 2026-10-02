package com.college.library.controller;

import com.college.library.dto.FineDtos.*;
import com.college.library.dto.PageResponse;
import com.college.library.entity.FineStatus;
import com.college.library.entity.PaymentMethod;
import com.college.library.service.FineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
@Tag(name = "Fines & payments")
public class FineController {

    private final FineService fineService;

    public FineController(FineService fineService) {
        this.fineService = fineService;
    }

    @GetMapping("/fines")
    public PageResponse<FineResponse> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) FineStatus status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return fineService.search(q, status, null, Paging.of(page, size, "createdAt", "desc"));
    }

    @GetMapping("/fines/member/{memberId}")
    public List<FineResponse> forMember(@PathVariable Long memberId) {
        return fineService.forMember(memberId);
    }

    @PostMapping("/fines/{id}/payment")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a (mock/offline) payment — no real money is processed")
    public PaymentResponse pay(@PathVariable Long id, @Valid @RequestBody PaymentRequest req) {
        return fineService.recordPayment(id, req);
    }

    @GetMapping("/fines/{id}/payments")
    public List<PaymentResponse> paymentsForFine(@PathVariable Long id) {
        return fineService.paymentsForFine(id);
    }

    @PostMapping("/fines/{id}/waive")
    @PreAuthorize("hasRole('ADMIN')")
    public FineResponse waive(@PathVariable Long id, @Valid @RequestBody WaiveRequest req) {
        return fineService.waive(id, req);
    }

    @GetMapping("/payments")
    public PageResponse<PaymentResponse> payments(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) PaymentMethod method,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return fineService.payments(q, method, from, to, Paging.of(page, size, "paymentDate", "desc"));
    }
}
