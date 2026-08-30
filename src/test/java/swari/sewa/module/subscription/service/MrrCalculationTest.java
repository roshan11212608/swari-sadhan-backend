package swari.sewa.module.subscription.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the MRR normalisation business rule implemented in
 * {@code SubscriptionDashboardServiceImpl}.
 *
 * <p>MRR (Monthly Recurring Revenue) is the sum, over every ACTIVE paid
 * subscription, of the price actually paid normalised to a monthly figure by
 * the billing cycle that was purchased. A 12,000 yearly plan contributes
 * 1,000/month; a 3,000 quarterly plan contributes 1,000/month. Trials are
 * excluded because they produce no recurring revenue.
 *
 * <p>The normalisation lives in private methods on the dashboard service. As
 * with {@code VatCalculationTest}, the rule is replicated here so the math can
 * be verified without spinning up a Spring context. If the production rule
 * changes, this test must change in lockstep.
 */
class MrrCalculationTest {

    // ===== Mirror of SubscriptionDashboardServiceImpl.monthsInCycle =====

    private int monthsInCycle(String billingCycle) {
        if (billingCycle == null) {
            return 1;
        }
        return switch (billingCycle.trim().toLowerCase()) {
            case "monthly" -> 1;
            case "quarterly" -> 3;
            case "halfyearly", "half_yearly", "half-yearly", "semiannual" -> 6;
            case "yearly", "annual" -> 12;
            default -> 0; // unknown cycles are skipped, not treated as 1
        };
    }

    // ===== Mirror of SubscriptionDashboardServiceImpl.calculateMrr =====

    private BigDecimal mrrFrom(List<Object[]> rows) {
        BigDecimal mrr = BigDecimal.ZERO;
        for (Object[] row : rows) {
            String cycle = (String) row[0];
            BigDecimal total = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
            int months = monthsInCycle(cycle);
            if (months <= 0) {
                continue;
            }
            mrr = mrr.add(total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP));
        }
        return mrr.setScale(2, RoundingMode.HALF_UP);
    }

    private static Object[] row(String cycle, String price) {
        return new Object[]{ cycle, new BigDecimal(price) };
    }

    // ===== Single-cycle normalisation =====

    @Test
    @DisplayName("Monthly 1000 -> MRR 1000.00")
    void testMonthly() {
        assertEquals(new BigDecimal("1000.00"),
                mrrFrom(Collections.singletonList(row("monthly", "1000"))));
    }

    @Test
    @DisplayName("Yearly 12000 -> MRR 1000.00 (12000 / 12)")
    void testYearly() {
        assertEquals(new BigDecimal("1000.00"),
                mrrFrom(Collections.singletonList(row("yearly", "12000"))));
    }

    @Test
    @DisplayName("Quarterly 3000 -> MRR 1000.00 (3000 / 3)")
    void testQuarterly() {
        assertEquals(new BigDecimal("1000.00"),
                mrrFrom(Collections.singletonList(row("quarterly", "3000"))));
    }

    @Test
    @DisplayName("Half-yearly 6000 -> MRR 1000.00 (6000 / 6)")
    void testHalfYearly() {
        assertEquals(new BigDecimal("1000.00"),
                mrrFrom(Collections.singletonList(row("halfyearly", "6000"))));
    }

    // ===== All half-yearly spellings map to 6 months =====

    @Test
    @DisplayName("All half-yearly spellings (halfyearly, half_yearly, half-yearly, semiannual) normalise to 6 months")
    void testHalfYearlySpellings() {
        for (String spelling : List.of("halfyearly", "half_yearly", "half-yearly", "semiannual")) {
            assertEquals(new BigDecimal("1000.00"),
                    mrrFrom(Collections.singletonList(row(spelling, "6000"))),
                    "Expected 6 months for spelling: " + spelling);
        }
    }

    @Test
    @DisplayName("yearly and annual both map to 12 months")
    void testAnnualSpellings() {
        assertEquals(new BigDecimal("1000.00"), mrrFrom(Collections.singletonList(row("yearly", "12000"))));
        assertEquals(new BigDecimal("1000.00"), mrrFrom(Collections.singletonList(row("annual",  "12000"))));
    }

    // ===== Portfolio aggregation =====

    @Test
    @DisplayName("Mixed portfolio: monthly 1000 + yearly 12000 + quarterly 3000 -> MRR 3000.00")
    void testMixedPortfolio() {
        BigDecimal mrr = mrrFrom(List.of(
                row("monthly",   "1000"),
                row("yearly",    "12000"),
                row("quarterly", "3000")
        ));
        assertEquals(new BigDecimal("3000.00"), mrr);
    }

    // ===== Edge cases =====

    @Test
    @DisplayName("Empty row list -> MRR 0.00 (not null, no exception)")
    void testEmpty() {
        assertEquals(new BigDecimal("0.00"), mrrFrom(Collections.emptyList()));
    }

    @Test
    @DisplayName("Unknown cycle 'biweekly' is skipped entirely -> contributes 0")
    void testUnknownCycleSkipped() {
        assertEquals(new BigDecimal("0.00"),
                mrrFrom(Collections.singletonList(row("biweekly", "500"))));
    }

    @Test
    @DisplayName("Unknown cycle mixed with a valid one only counts the valid one")
    void testUnknownCycleMixed() {
        BigDecimal mrr = mrrFrom(List.of(
                row("biweekly", "99999"),
                row("monthly",  "1000")
        ));
        assertEquals(new BigDecimal("1000.00"), mrr);
    }

    @Test
    @DisplayName("null billing cycle is treated as monthly (1 month)")
    void testNullCycle() {
        assertEquals(new BigDecimal("1000.00"),
                mrrFrom(Collections.singletonList(row(null, "1000"))));
    }

    @Test
    @DisplayName("Case insensitivity: YEARLY and Yearly behave as yearly")
    void testCaseInsensitive() {
        assertEquals(new BigDecimal("1000.00"), mrrFrom(Collections.singletonList(row("YEARLY", "12000"))));
        assertEquals(new BigDecimal("1000.00"), mrrFrom(Collections.singletonList(row("Yearly", "12000"))));
    }

    @Test
    @DisplayName("Whitespace tolerance: ' monthly ' behaves as monthly")
    void testWhitespaceTolerance() {
        assertEquals(new BigDecimal("1000.00"),
                mrrFrom(Collections.singletonList(row(" monthly ", "1000"))));
    }

    // ===== Rounding =====

    @Test
    @DisplayName("Rounding: yearly 10000 / 12 = 833.333... rounds HALF_UP to 833.33")
    void testRoundingYearly() {
        assertEquals(new BigDecimal("833.33"),
                mrrFrom(Collections.singletonList(row("yearly", "10000"))));
    }

    @Test
    @DisplayName("Rounding: quarterly 1000 / 3 = 333.333... rounds to 333.33")
    void testRoundingQuarterly() {
        assertEquals(new BigDecimal("333.33"),
                mrrFrom(Collections.singletonList(row("quarterly", "1000"))));
    }

    @Test
    @DisplayName("Result scale is always exactly 2")
    void testScaleIsTwo() {
        BigDecimal mrr = mrrFrom(Collections.singletonList(row("monthly", "1000")));
        assertEquals(2, mrr.scale());
    }

    @Test
    @DisplayName("ARR relationship: ARR = MRR * 12 (monthly 1000 -> ARR 12000.00)")
    void testArrRelationship() {
        BigDecimal mrr = mrrFrom(Collections.singletonList(row("monthly", "1000")));
        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("12000.00"), arr);
    }

    @Test
    @DisplayName("A zero pricePaid contributes 0 and does not throw")
    void testZeroPrice() {
        BigDecimal mrr = mrrFrom(List.of(
                row("monthly", "0"),
                row("yearly",  "12000")
        ));
        assertEquals(new BigDecimal("1000.00"), mrr);
    }
}
