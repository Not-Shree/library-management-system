package com.college.library.repository;

import com.college.library.entity.ReturnRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReturnRecordRepository extends JpaRepository<ReturnRecord, Long> {

    List<ReturnRecord> findByBorrowing_IdIn(Collection<Long> borrowingIds);

    Optional<ReturnRecord> findByBorrowing_Id(Long borrowingId);

    long countByReturnDate(LocalDate date);

    List<ReturnRecord> findByReturnDateBetweenOrderByReturnDateDesc(LocalDate from, LocalDate to);

    @Query(value = """
            select to_char(date_trunc('month', return_date), 'YYYY-MM') as month, count(*)
            from returns where return_date >= :from group by 1 order by 1""", nativeQuery = true)
    List<Object[]> returnedPerMonth(@Param("from") LocalDate from);
}
