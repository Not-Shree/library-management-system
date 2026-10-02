package com.college.library.repository;

import com.college.library.entity.Fine;
import com.college.library.entity.FineStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long>, JpaSpecificationExecutor<Fine> {

    List<Fine> findByBorrowing_IdIn(Collection<Long> borrowingIds);

    Optional<Fine> findByBorrowing_Id(Long borrowingId);

    List<Fine> findByBorrowing_Member_IdOrderByCreatedAtDesc(Long memberId);

    List<Fine> findByStatusInOrderByCreatedAtAsc(Collection<FineStatus> statuses);

    /** Row lock so two payments for the same fine cannot be recorded at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fine f where f.id = :id")
    Optional<Fine> findByIdForUpdate(@Param("id") Long id);

    @Query("select coalesce(sum(f.amount - f.paidAmount - f.waivedAmount), 0) from Fine f where f.borrowing.member.id = :memberId")
    BigDecimal outstandingForMember(@Param("memberId") Long memberId);

    @Query("select coalesce(sum(f.amount - f.paidAmount - f.waivedAmount), 0) from Fine f")
    BigDecimal totalOutstanding();

    /** Rows of [memberId, outstanding] for a page of members. */
    @Query("""
            select f.borrowing.member.id, sum(f.amount - f.paidAmount - f.waivedAmount) from Fine f
            where f.borrowing.member.id in :memberIds group by f.borrowing.member.id""")
    List<Object[]> outstandingByMemberIds(@Param("memberIds") Collection<Long> memberIds);
}
