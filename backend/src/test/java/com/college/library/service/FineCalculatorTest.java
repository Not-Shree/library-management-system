package com.college.library.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Plain unit tests: no Spring context and no database needed. Run with: mvn test */
class FineCalculatorTest {

    private final FineCalculator calculator = new FineCalculator();
    private final FinePolicy defaults = new FinePolicy(new BigDecimal("5.00"), new BigDecimal("500.00"), 0);

    private static LocalDate oct(int day) {
        return LocalDate.of(2026, 10, day);
    }

    @Test
    void returnedOnDueDate_noFine() {
        FineResult r = calculator.calculate(oct(10), oct(10), defaults);
        assertEquals(0, r.overdueDays());
        assertEquals(new BigDecimal("0.00"), r.amount());
        assertFalse(r.hasFine());
    }

    @Test
    void returnedEarly_noFine() {
        assertFalse(calculator.calculate(oct(10), oct(3), defaults).hasFine());
    }

    @Test
    void exampleFromSpecification_fiveDaysLateIs25Rupees() {
        FineResult r = calculator.calculate(oct(10), oct(15), defaults);
        assertEquals(5, r.overdueDays());
        assertEquals(new BigDecimal("25.00"), r.amount());
        assertFalse(r.capped());
    }

    @Test
    void fineIsCappedAtMaximum() {
        FineResult r = calculator.calculate(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), defaults);
        assertEquals(151, r.overdueDays());
        assertEquals(new BigDecimal("500.00"), r.amount());
        assertTrue(r.capped());
    }

    @Test
    void gracePeriodDaysAreNotCharged() {
        FinePolicy withGrace = new FinePolicy(new BigDecimal("5.00"), new BigDecimal("500.00"), 2);
        FineResult r = calculator.calculate(oct(10), oct(15), withGrace);
        assertEquals(5, r.overdueDays());
        assertEquals(3, r.chargeableDays());
        assertEquals(new BigDecimal("15.00"), r.amount());
    }

    @Test
    void lateButInsideGracePeriod_noFine() {
        FinePolicy withGrace = new FinePolicy(new BigDecimal("5.00"), new BigDecimal("500.00"), 3);
        assertFalse(calculator.calculate(oct(10), oct(12), withGrace).hasFine());
    }

    @Test
    void usesConfiguredRateNotAHardCodedOne() {
        FinePolicy tenRupees = new FinePolicy(new BigDecimal("10.00"), new BigDecimal("1000.00"), 0);
        assertEquals(new BigDecimal("50.00"), calculator.calculate(oct(10), oct(15), tenRupees).amount());
    }

    @Test
    void missingInput_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(null, oct(1), defaults));
    }
}
