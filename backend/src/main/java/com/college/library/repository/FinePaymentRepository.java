package com.college.library.repository;

import com.college.library.entity.FinePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface FinePaymentRepository extends JpaRepository<FinePayment, Long>, JpaSpecificationExecutor<FinePayment> {

    List<FinePayment> findByFine_Borrowing_Member_IdOrderByPaymentDateDesc(Long memberId);

    List<FinePayment> findByFine_IdOrderByPaymentDateAsc(Long fineId);

    List<FinePayment> findByPaymentDateBetweenOrderByPaymentDateDesc(LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(p.amount), 0) from FinePayment p")
    BigDecimal totalCollected();

    @Query("select coalesce(sum(p.amount), 0) from FinePayment p where p.paymentDate >= :from and p.paymentDate < :to")
    BigDecimal collectedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Rows of ["YYYY-MM", amount]. */
    @Query(value = """
            select to_char(date_trunc('month', payment_date), 'YYYY-MM') as month, sum(amount)
            from fine_payments where payment_date >= :from group by 1 order by 1""", nativeQuery = true)
    List<Object[]> collectedPerMonth(@Param("from") LocalDate from);
}
