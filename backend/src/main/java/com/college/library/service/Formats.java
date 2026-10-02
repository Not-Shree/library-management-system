package com.college.library.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Formatting used in notification and audit messages. */
final class Formats {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private Formats() {
    }

    static String date(LocalDate d) {
        return d == null ? "-" : d.format(DATE);
    }

    static String money(BigDecimal amount) {
        return "₹" + (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
