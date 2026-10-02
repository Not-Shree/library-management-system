package com.college.library.service;

import java.math.BigDecimal;

/**
 * Output of {@link FineCalculator}.
 *
 * @param overdueDays    days between the due date and the return date (0 if on time)
 * @param chargeableDays overdue days minus the grace period
 * @param amount         final fine, already capped
 * @param capped         true when the maximum fine limit was applied
 */
public record FineResult(int overdueDays, int chargeableDays, BigDecimal amount, boolean capped) {

    public static FineResult noFine() {
        return new FineResult(0, 0, BigDecimal.ZERO.setScale(2), false);
    }

    public boolean hasFine() {
        return amount.signum() > 0;
    }
}
