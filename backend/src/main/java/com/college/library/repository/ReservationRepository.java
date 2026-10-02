package com.college.library.repository;

import com.college.library.entity.Reservation;
import com.college.library.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    /** Head of the first-come-first-served queue for a book. */
    Optional<Reservation> findFirstByBook_IdAndStatusOrderByReservedAtAscIdAsc(Long bookId, ReservationStatus status);

    /** Whole waiting queue for a book, oldest first. */
    List<Reservation> findByBook_IdAndStatusOrderByReservedAtAscIdAsc(Long bookId, ReservationStatus status);

    boolean existsByBook_IdAndStatusIn(Long bookId, Collection<ReservationStatus> statuses);

    boolean existsByBook_IdAndStatusInAndMember_IdNot(Long bookId, Collection<ReservationStatus> statuses, Long memberId);

    Optional<Reservation> findFirstByMember_IdAndBook_IdAndStatusIn(Long memberId, Long bookId,
                                                                      Collection<ReservationStatus> statuses);

    Optional<Reservation> findFirstByHeldCopy_IdAndStatus(Long copyId, ReservationStatus status);

    List<Reservation> findByBook_IdAndStatusIn(Long bookId, Collection<ReservationStatus> statuses);

    List<Reservation> findByStatusAndExpiryDateBefore(ReservationStatus status, LocalDate date);

    List<Reservation> findByMember_IdOrderByReservedAtDesc(Long memberId);

    List<Reservation> findByReservedAtBetweenOrderByReservedAtDesc(LocalDateTime from, LocalDateTime to);

    long countByStatus(ReservationStatus status);

    long countByStatusIn(Collection<ReservationStatus> statuses);

    long countByBook_IdAndStatusIn(Long bookId, Collection<ReservationStatus> statuses);

    long countByMember_IdAndStatus(Long memberId, ReservationStatus status);

    long countByMember_IdAndStatusIn(Long memberId, Collection<ReservationStatus> statuses);

    /** People ahead in the queue: used to show "position 2 of 3". */
    long countByBook_IdAndStatusAndReservedAtBefore(Long bookId, ReservationStatus status, LocalDateTime reservedAt);
}
