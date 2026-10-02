package com.college.library.service;

import java.math.BigDecimal;

/**
 * The fine rules taken from the library settings table.
 *
 * @param finePerDay      amount charged for each chargeable late day (e.g. ₹5)
 * @param maxFinePerBook  upper limit for a single borrowing (e.g. ₹500)
 * @param gracePeriodDays number of late days that are forgiven before charging starts
 */
public record FinePolicy(BigDecimal finePerDay, BigDecimal maxFinePerBook, int gracePeriodDays) {
}
