package com.college.library.controller;

import com.college.library.dto.PageResponse;
import com.college.library.dto.ReservationDtos.*;
import com.college.library.entity.ReservationStatus;
import com.college.library.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reserve a book with no free copy (members reserve for themselves; staff pass memberId)")
    public ReservationResponse create(@Valid @RequestBody ReservationRequest req) {
        return reservationService.create(req);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public PageResponse<ReservationResponse> search(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) ReservationStatus status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return reservationService.search(q, status, Paging.of(page, size, "reservedAt", "asc"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a reservation (staff, or the member who made it)")
    public ReservationResponse cancel(@PathVariable Long id) {
        return reservationService.cancel(id);
    }
}
