package com.college.library.controller;

import com.college.library.dto.CirculationDtos.BorrowingResponse;
import com.college.library.dto.FineDtos.FineResponse;
import com.college.library.dto.FineDtos.PaymentResponse;
import com.college.library.dto.MemberDtos.MemberResponse;
import com.college.library.dto.MemberDtos.ProfileUpdateRequest;
import com.college.library.dto.MiscDtos.NotificationResponse;
import com.college.library.dto.PageResponse;
import com.college.library.dto.ReservationDtos.ReservationResponse;
import com.college.library.security.CurrentUserService;
import com.college.library.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Self-service endpoints for the logged-in member. A member can never see another member's data. */
@RestController
@RequestMapping("/api/me")
@PreAuthorize("hasRole('MEMBER')")
@Tag(name = "Member self-service")
public class MeController {

    private final CurrentUserService currentUser;
    private final MemberService memberService;
    private final BorrowingService borrowingService;
    private final FineService fineService;
    private final ReservationService reservationService;
    private final NotificationService notificationService;

    public MeController(CurrentUserService currentUser, MemberService memberService, BorrowingService borrowingService,
                        FineService fineService, ReservationService reservationService,
                        NotificationService notificationService) {
        this.currentUser = currentUser;
        this.memberService = memberService;
        this.borrowingService = borrowingService;
        this.fineService = fineService;
        this.reservationService = reservationService;
        this.notificationService = notificationService;
    }

    private Long memberId() {
        return currentUser.requireCurrentMember().getId();
    }

    @GetMapping
    public MemberResponse profile() {
        return memberService.myProfile();
    }

    @PutMapping
    public MemberResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest req) {
        return memberService.updateMyProfile(req);
    }

    @GetMapping("/borrowings/current")
    public List<BorrowingResponse> current() {
        return borrowingService.currentForMember(memberId());
    }

    @GetMapping("/borrowings")
    public PageResponse<BorrowingResponse> history(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return borrowingService.search(null, null, false, memberId(), Paging.of(page, size, "issueDate", "desc"));
    }

    @GetMapping("/fines")
    public List<FineResponse> fines() {
        return fineService.forMember(memberId());
    }

    @GetMapping("/payments")
    public List<PaymentResponse> payments() {
        return fineService.paymentsForMember(memberId());
    }

    @GetMapping("/reservations")
    public List<ReservationResponse> reservations() {
        return reservationService.forMember(memberId());
    }

    @GetMapping("/notifications")
    public PageResponse<NotificationResponse> notifications(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "15") int size) {
        return notificationService.forMember(memberId(), Paging.of(page, size, "createdAt", "desc"));
    }

    @GetMapping("/notifications/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notificationService.unreadCount(memberId()));
    }

    @PatchMapping("/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id) {
        notificationService.markRead(memberId(), id);
    }

    @PatchMapping("/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead() {
        notificationService.markAllRead(memberId());
    }
}
