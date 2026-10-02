package com.college.library.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calculates late-return fines. This class has no database access, so it is easy to test
 * and to explain:
 *
 * <pre>
 *   if returnDate &lt;= dueDate           -> fine = 0
 *   overdueDays    = returnDate - dueDate
 *   chargeableDays = max(0, overdueDays - gracePeriodDays)
 *   fine           = min(chargeableDays × finePerDay, maxFinePerBook)
 * </pre>
 *
 * Example (default settings: ₹5/day, no grace, ₹500 cap):
 * due 10 Oct, returned 15 Oct -> 5 days late -> 5 × ₹5 = ₹25.
 */
@Component
public class FineCalculator {

    public FineResult calculate(LocalDate dueDate, LocalDate returnDate, FinePolicy policy) {
        if (dueDate == null || returnDate == null || policy == null) {
            throw new IllegalArgumentException("Due date, return date and fine policy are required");
        }
        // Returned on or before the due date: no fine at all.
        if (!returnDate.isAfter(dueDate)) {
            return FineResult.noFine();
        }

        int overdueDays = (int) ChronoUnit.DAYS.between(dueDate, returnDate);
        int chargeableDays = Math.max(0, overdueDays - policy.gracePeriodDays());

        BigDecimal rawFine = policy.finePerDay().multiply(BigDecimal.valueOf(chargeableDays));
        boolean capped = rawFine.compareTo(policy.maxFinePerBook()) > 0;
        BigDecimal fine = (capped ? policy.maxFinePerBook() : rawFine).setScale(2, RoundingMode.HALF_UP);

        return new FineResult(overdueDays, chargeableDays, fine, capped);
    }
}
