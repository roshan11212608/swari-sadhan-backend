package swari.sewa.module.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VAT calculation logic.
 *
 * Verifies the business rule:
 *   Plan price
 *   - Coupon discount
 *   = Taxable amount
 *   × VAT rate
 *   = VAT
 *   Taxable amount + VAT
 *   = Final amount
 *
 * All calculations must use BigDecimal with HALF_UP rounding to 2 decimal places.
 */
class VatCalculationTest {

    /**
     * Replicates the VAT calculation from EsewaPaymentServiceImpl.createPayment
     * to verify the math is correct.
     */
    private BigDecimal[] calculatePayment(BigDecimal planPrice, BigDecimal discountAmount, Integer vatPercentage) {
        BigDecimal amountAfterDiscount = planPrice.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount;
        if (vatPercentage != null && vatPercentage > 0) {
            taxAmount = amountAfterDiscount.multiply(BigDecimal.valueOf(vatPercentage))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal totalAmount = amountAfterDiscount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal[]{amountAfterDiscount, taxAmount, totalAmount};
    }

    @Test
    @DisplayName("No coupon, 13% VAT: 2699 → tax 350.87, total 3049.87")
    void testVat_noCoupon() {
        BigDecimal[] result = calculatePayment(new BigDecimal("2699"), BigDecimal.ZERO, 13);
        assertEquals(new BigDecimal("2699.00"), result[0]); // taxable
        assertEquals(new BigDecimal("350.87"), result[1]);  // VAT
        assertEquals(new BigDecimal("3049.87"), result[2]); // total
    }

    @Test
    @DisplayName("10% coupon + 13% VAT: 2699 - 269.90 = 2429.10 → tax 315.78, total 2744.88")
    void testVat_withCoupon() {
        BigDecimal discount = new BigDecimal("2699").multiply(BigDecimal.TEN)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal[] result = calculatePayment(new BigDecimal("2699"), discount, 13);
        assertEquals(new BigDecimal("2429.10"), result[0]); // taxable
        assertEquals(new BigDecimal("315.78"), result[1]);  // VAT (2429.10 * 0.13 = 315.783 → 315.78)
        assertEquals(new BigDecimal("2744.88"), result[2]); // total
    }

    @Test
    @DisplayName("VAT disabled: 2699 → tax 0, total 2699")
    void testVat_disabled() {
        BigDecimal[] result = calculatePayment(new BigDecimal("2699"), BigDecimal.ZERO, 0);
        assertEquals(new BigDecimal("2699.00"), result[0]);
        assertEquals(new BigDecimal("0.00"), result[1]);
        assertEquals(new BigDecimal("2699.00"), result[2]);
    }

    @Test
    @DisplayName("Full discount (100% coupon) + VAT: 1000 - 1000 = 0 → tax 0, total 0")
    void testVat_fullDiscount() {
        BigDecimal[] result = calculatePayment(new BigDecimal("1000"), new BigDecimal("1000"), 13);
        assertEquals(new BigDecimal("0.00"), result[0]); // taxable
        assertEquals(new BigDecimal("0.00"), result[1]); // VAT
        assertEquals(new BigDecimal("0.00"), result[2]); // total
    }

    @Test
    @DisplayName("Flat 500 coupon + 18% VAT: 2699 - 500 = 2199 → tax 395.82, total 2594.82")
    void testVat_flatCoupon_18pct() {
        BigDecimal[] result = calculatePayment(new BigDecimal("2699"), new BigDecimal("500"), 18);
        assertEquals(new BigDecimal("2199.00"), result[0]); // taxable
        assertEquals(new BigDecimal("395.82"), result[1]);  // VAT (2199 * 0.18 = 395.82)
        assertEquals(new BigDecimal("2594.82"), result[2]); // total
    }

    @Test
    @DisplayName("VAT is calculated AFTER discount, not on original price")
    void testVat_afterDiscount() {
        // Verify: 10% coupon on 1000 → 100 discount → 900 taxable → 13% VAT = 117 → total 1017
        // NOT: 1000 * 13% = 130 → total 1000 - 100 + 130 = 1030
        BigDecimal discount = new BigDecimal("1000").multiply(BigDecimal.TEN)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal[] result = calculatePayment(new BigDecimal("1000"), discount, 13);
        assertEquals(new BigDecimal("900.00"), result[0]);
        assertEquals(new BigDecimal("117.00"), result[1]);  // 900 * 0.13 = 117.00
        assertEquals(new BigDecimal("1017.00"), result[2]); // 900 + 117 = 1017
    }

    @Test
    @DisplayName("Rounding consistency: all amounts have exactly 2 decimal places")
    void testRoundingConsistency() {
        BigDecimal[] result = calculatePayment(new BigDecimal("2699"), new BigDecimal("269.90"), 13);
        for (BigDecimal amount : result) {
            assertEquals(2, amount.scale(), "Amount should have exactly 2 decimal places: " + amount);
        }
    }

    @Test
    @DisplayName("Total never negative: 100% coupon + VAT → total = 0.00 (not negative)")
    void testTotalNeverNegative() {
        BigDecimal[] result = calculatePayment(new BigDecimal("500"), new BigDecimal("500"), 13);
        assertEquals(new BigDecimal("0.00"), result[2]);
        assertTrue(result[2].compareTo(BigDecimal.ZERO) >= 0, "Total must never be negative");
    }
}
