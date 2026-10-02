package com.college.library.service;

import com.college.library.dto.PageResponse;
import com.college.library.dto.ReservationDtos.ReservationRequest;
import com.college.library.dto.ReservationDtos.ReservationResponse;
import com.college.library.entity.*;
import com.college.library.exception.BusinessRuleException;
import com.college.library.exception.DuplicateResourceException;
import com.college.library.exception.ResourceNotFoundException;
import com.college.library.mapper.CirculationMapper;
import com.college.library.repository.*;
import com.college.library.security.AppUserPrincipal;
import com.college.library.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Reservation queue (first come, first served).
 *
 * WAITING   -> no copy free yet, member is in the queue
 * AVAILABLE -> a returned copy is held for the member until expiryDate
 * FULFILLED -> the member borrowed the book
 * CANCELLED / EXPIRED -> left the queue
 */
@Service
public class ReservationService {

    static final List<ReservationStatus> ACTIVE = List.of(ReservationStatus.WAITING, ReservationStatus.AVAILABLE);
    private static final List<CopyStatus> NOT_IN_LIBRARY = List.of(CopyStatus.WITHDRAWN, CopyStatus.LOST);

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final MemberRepository memberRepository;
    private final BorrowingRepository borrowingRepository;
    private final SettingsService settingsService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public ReservationService(ReservationRepository reservationRepository, BookRepository bookRepository,
                              BookCopyRepository copyRepository, MemberRepository memberRepository,
                              BorrowingRepository borrowingRepository, SettingsService settingsService,
                              NotificationService notificationService, AuditService auditService,
                              CurrentUserService currentUser) {
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.copyRepository = copyRepository;
        this.memberRepository = memberRepository;
        this.borrowingRepository = borrowingRepository;
        this.settingsService = settingsService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional
    public ReservationResponse create(ReservationRequest req) {
        AppUserPrincipal caller = currentUser.requirePrincipal();
        Member member;
        if (caller.isStaff()) {
            if (req.memberId() == null) {
                throw new BusinessRuleException("MEMBER_REQUIRED", "Choose the member who is reserving the book");
            }
            member = memberRepository.findById(req.memberId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Member", req.memberId()));
        } else {
            member = currentUser.requireCurrentMember();
        }
        if (!member.isActive()) {
            throw new BusinessRuleException("MEMBER_INACTIVE", "Membership is inactive, so books cannot be reserved");
        }
        Book book = bookRepository.findByIdAndDeletedFalse(req.bookId())
                .orElseThrow(() -> ResourceNotFoundException.of("Book", req.bookId()));

        if (copyRepository.countByBook_IdAndStatusNotIn(book.getId(), NOT_IN_LIBRARY) == 0) {
            throw new BusinessRuleException("RESERVATION_NOT_ALLOWED", "The library has no copies of this book yet");
        }
        // Rule: a member cannot reserve a book while a copy is on the shelf.
        if (copyRepository.existsByBook_IdAndStatus(book.getId(), CopyStatus.AVAILABLE)) {
            throw new BusinessRuleException("RESERVATION_NOT_ALLOWED",
                    "A copy is available on the shelf, so it can be borrowed straight away");
        }
        if (borrowingRepository.existsByMember_IdAndBookCopy_Book_IdAndStatus(member.getId(), book.getId(), BorrowingStatus.BORROWED)) {
            throw new BusinessRuleException("RESERVATION_NOT_ALLOWED", member.getFullName() + " already has a copy of this book");
        }
        if (reservationRepository.findFirstByMember_IdAndBook_IdAndStatusIn(member.getId(), book.getId(), ACTIVE).isPresent()) {
            throw new DuplicateResourceException("DUPLICATE_RESERVATION", member.getFullName() + " is already in the queue for this book");
        }

        Reservation r = new Reservation();
        r.setMember(member);
        r.setBook(book);
        r.setReservedAt(LocalDateTime.now());
        r.setStatus(ReservationStatus.WAITING);
        reservationRepository.save(r);

        auditService.log("CREATE_RESERVATION", "Reservation", r.getId(),
                member.getMemberCode() + " reserved \"" + book.getTitle() + "\"");
        return toResponse(r);
    }

    @Transactional
    public ReservationResponse cancel(Long id) {
        Reservation r = reservationRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Reservation", id));
        AppUserPrincipal caller = currentUser.requirePrincipal();
        if (!caller.isStaff() && !isOwnedBy(r.getMember(), caller)) {
            throw new BusinessRuleException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can only cancel your own reservations");
        }
        if (!r.isActive()) {
            throw new BusinessRuleException("INVALID_RESERVATION", "Only waiting or ready reservations can be cancelled");
        }
        BookCopy held = r.getStatus() == ReservationStatus.AVAILABLE ? r.getHeldCopy() : null;
        r.setStatus(ReservationStatus.CANCELLED);
        if (held != null) {
            offerCopyToQueue(held);  // give the held copy to the next person, or put it back on the shelf
        }
        auditService.log("CANCEL_RESERVATION", "Reservation", r.getId(),
                r.getMember().getMemberCode() + " left the queue for \"" + r.getBook().getTitle() + "\"");
        return toResponse(r);
    }

    /**
     * Called whenever a copy becomes free (return, repair, new copy, cancelled hold).
     * The copy is held for the first eligible (active) member in the queue; if nobody is
     * waiting, it goes back on the shelf as AVAILABLE.
     *
     * @return the reservation that received the copy, or empty if the copy went to the shelf
     */
    @Transactional
    public Optional<Reservation> offerCopyToQueue(BookCopy copy) {
        List<Reservation> queue = reservationRepository.findByBook_IdAndStatusOrderByReservedAtAscIdAsc(
                copy.getBook().getId(), ReservationStatus.WAITING);
        Optional<Reservation> next = queue.stream().filter(r -> r.getMember().isActive()).findFirst();
        if (next.isEmpty()) {
            copy.setStatus(CopyStatus.AVAILABLE);
            return Optional.empty();
        }
        Reservation r = next.get();
        LocalDate expiry = LocalDate.now().plusDays(settingsService.current().getReservationHoldDays());
        copy.setStatus(CopyStatus.RESERVED);
        r.setStatus(ReservationStatus.AVAILABLE);
        r.setHeldCopy(copy);
        r.setAvailableAt(LocalDateTime.now());
        r.setExpiryDate(expiry);
        notificationService.notify(r.getMember(), NotificationType.RESERVATION_AVAILABLE, "Reserved book is ready",
                "\"" + copy.getBook().getTitle() + "\" (copy " + copy.getCopyCode() + ") is being held for you. "
                        + "Collect it by " + Formats.date(expiry) + ".");
        return Optional.of(r);
    }

    /**
     * Called by the issue process: if the member had a reservation for this book it is FULFILLED.
     * If a different copy was being held for them, that held copy is released to the next person.
     */
    @Transactional
    public void markFulfilledOnIssue(Member member, BookCopy issuedCopy) {
        reservationRepository.findFirstByMember_IdAndBook_IdAndStatusIn(member.getId(), issuedCopy.getBook().getId(), ACTIVE)
                .ifPresent(r -> {
                    BookCopy held = r.getHeldCopy();
                    r.setStatus(ReservationStatus.FULFILLED);
                    if (held != null && r.getAvailableAt() != null && !held.getId().equals(issuedCopy.getId())
                            && held.getStatus() == CopyStatus.RESERVED) {
                        offerCopyToQueue(held);
                    }
                    r.setHeldCopy(issuedCopy);
                });
    }

    /** Daily job: holds that were not collected in time expire and the copy moves on. */
    @Transactional
    public int expireUncollectedHolds() {
        List<Reservation> expired = reservationRepository.findByStatusAndExpiryDateBefore(
                ReservationStatus.AVAILABLE, LocalDate.now());
        for (Reservation r : expired) {
            r.setStatus(ReservationStatus.EXPIRED);
            notificationService.notify(r.getMember(), NotificationType.RESERVATION_EXPIRED, "Reservation expired",
                    "Your hold on \"" + r.getBook().getTitle() + "\" expired because it was not collected by "
                            + Formats.date(r.getExpiryDate()) + ".");
            if (r.getHeldCopy() != null) {
                offerCopyToQueue(r.getHeldCopy());
            }
            auditService.log("EXPIRE_RESERVATION", "Reservation", r.getId(), "Hold not collected in time");
        }
        return expired.size();
    }

    /** Used when a book is removed from the catalogue. */
    @Transactional
    public void cancelAllForBook(Long bookId) {
        for (Reservation r : reservationRepository.findByBook_IdAndStatusIn(bookId, ACTIVE)) {
            r.setStatus(ReservationStatus.CANCELLED);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> search(String q, ReservationStatus status, Pageable pageable) {
        Page<Reservation> page = reservationRepository.findAll(SearchSpecs.reservations(q, status), pageable);
        return PageResponse.of(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> forMember(Long memberId) {
        return reservationRepository.findByMember_IdOrderByReservedAtDesc(memberId).stream().map(this::toResponse).toList();
    }

    ReservationResponse toResponse(Reservation r) {
        Integer position = null;
        if (r.getStatus() == ReservationStatus.WAITING) {
            position = (int) reservationRepository.countByBook_IdAndStatusAndReservedAtBefore(
                    r.getBook().getId(), ReservationStatus.WAITING, r.getReservedAt()) + 1;
        }
        return CirculationMapper.toReservationResponse(r, position);
    }

    private static boolean isOwnedBy(Member m, AppUserPrincipal p) {
        return m.getUser() != null && m.getUser().getId().equals(p.getId());
    }
}
