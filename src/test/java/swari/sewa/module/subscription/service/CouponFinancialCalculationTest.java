package swari.sewa.module.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import swari.sewa.module.subscription.dto.CouponValidationResponse;
import swari.sewa.module.subscription.entity.SubscriptionCoupon;
import swari.sewa.module.subscription.enums.CouponDiscountType;
import swari.sewa.module.subscription.repository.SubscriptionCouponRepository;
import swari.sewa.module.subscription.repository.SubscriptionCouponUsageRepository;
import swari.sewa.module.subscription.service.impl.SubscriptionCouponServiceImpl;
import swari.sewa.module.subscription.service.SubscriptionAuditService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit tests for coupon financial calculations.
 *
 * These tests verify that the coupon validation logic correctly calculates
 * discounts, enforces maximum discount caps, prevents negative final amounts,
 * and handles edge cases — all using BigDecimal (no floating-point).
 *
 * The tests mock the repositories so no database is required.
 */
class CouponFinancialCalculationTest {

    private SubscriptionCouponServiceImpl couponService;
    private SubscriptionCouponRepository couponRepository;
    private SubscriptionCouponUsageRepository usageRepository;

    @BeforeEach
    void setUp() {
        couponRepository = Mockito.mock(SubscriptionCouponRepository.class);
        usageRepository = Mockito.mock(SubscriptionCouponUsageRepository.class);
        SubscriptionAuditService auditService = Mockito.mock(SubscriptionAuditService.class);
        couponService = new SubscriptionCouponServiceImpl(couponRepository, usageRepository, auditService);
    }

    private SubscriptionCoupon createPercentageCoupon(String code, Integer percentage, BigDecimal maxDiscount, BigDecimal minPurchase, Integer usageLimit) {
        return SubscriptionCoupon.builder()
                .id(1L)
                .code(code)
                .discountType(CouponDiscountType.PERCENTAGE)
                .percentage(percentage)
                .maximumDiscount(maxDiscount)
                .minimumPurchase(minPurchase)
                .usageLimit(usageLimit != null ? usageLimit : 100)
                .active(true)
                .build();
    }

    private SubscriptionCoupon createFlatCoupon(String code, BigDecimal flatDiscount, BigDecimal minPurchase, Integer usageLimit) {
        return SubscriptionCoupon.builder()
                .id(1L)
                .code(code)
                .discountType(CouponDiscountType.FLAT)
                .flatDiscount(flatDiscount)
                .minimumPurchase(minPurchase)
                .usageLimit(usageLimit != null ? usageLimit : 100)
                .active(true)
                .build();
    }

    // ===== Percentage Discount Tests =====

    @Test
    @DisplayName("Percentage coupon: 10% off NPR 2,699 = NPR 269.90 discount")
    void testPercentageDiscount_basic() {
        SubscriptionCoupon coupon = createPercentageCoupon("SAVE10", 10, null, null, 100);
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("SAVE10", new BigDecimal("2699"));

        assertTrue(result.getValid());
        assertEquals(new BigDecimal("269.90"), result.getDiscountAmount());
        assertEquals(new BigDecimal("2429.10"), result.getFinalAmount());
    }

    @Test
    @DisplayName("Percentage coupon with maximum discount cap: 10% off 5000, max 200 → discount capped at 200")
    void testPercentageDiscount_maxCap() {
        SubscriptionCoupon coupon = createPercentageCoupon("CAP10", 10, new BigDecimal("200"), null, 100);
        when(couponRepository.findByCode("CAP10")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("CAP10", new BigDecimal("5000"));

        assertTrue(result.getValid());
        // 10% of 5000 = 500, but max is 200 → discount = 200
        assertEquals(new BigDecimal("200.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("4800.00"), result.getFinalAmount());
    }

    @Test
    @DisplayName("Percentage coupon: 50% off 1000, no max → discount = 500")
    void testPercentageDiscount_half() {
        SubscriptionCoupon coupon = createPercentageCoupon("HALF", 50, null, null, 100);
        when(couponRepository.findByCode("HALF")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("HALF", new BigDecimal("1000"));

        assertTrue(result.getValid());
        assertEquals(new BigDecimal("500.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("500.00"), result.getFinalAmount());
    }

    // ===== Flat Discount Tests =====

    @Test
    @DisplayName("Flat coupon: NPR 500 off NPR 2,699 = NPR 2,199 final")
    void testFlatDiscount_basic() {
        SubscriptionCoupon coupon = createFlatCoupon("FLAT500", new BigDecimal("500"), null, 100);
        when(couponRepository.findByCode("FLAT500")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("FLAT500", new BigDecimal("2699"));

        assertTrue(result.getValid());
        assertEquals(new BigDecimal("500.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("2199.00"), result.getFinalAmount());
    }

    // ===== Discount Cannot Exceed Price =====

    @Test
    @DisplayName("Flat discount exceeding price: NPR 5000 off NPR 1000 → discount capped at 1000, final = 0")
    void testDiscountExceedsPrice_flat() {
        SubscriptionCoupon coupon = createFlatCoupon("HUGE", new BigDecimal("5000"), null, 100);
        when(couponRepository.findByCode("HUGE")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("HUGE", new BigDecimal("1000"));

        assertTrue(result.getValid());
        // Discount capped at the plan price
        assertEquals(new BigDecimal("1000.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("0.00"), result.getFinalAmount());
    }

    @Test
    @DisplayName("Percentage 100% off → final = 0 (not negative)")
    void testDiscountFullPrice_percentage() {
        SubscriptionCoupon coupon = createPercentageCoupon("FREE", 100, null, null, 100);
        when(couponRepository.findByCode("FREE")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("FREE", new BigDecimal("999"));

        assertTrue(result.getValid());
        assertEquals(new BigDecimal("999.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("0.00"), result.getFinalAmount());
    }

    // ===== Minimum Purchase Tests =====

    @Test
    @DisplayName("Minimum purchase met: plan price 2699 >= min 2000 → eligible")
    void testMinimumPurchase_met() {
        SubscriptionCoupon coupon = createPercentageCoupon("MIN", 10, null, new BigDecimal("2000"), 100);
        when(couponRepository.findByCode("MIN")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("MIN", new BigDecimal("2699"));

        assertTrue(result.getValid());
        assertEquals(new BigDecimal("269.90"), result.getDiscountAmount());
    }

    @Test
    @DisplayName("Minimum purchase not met: plan price 1500 < min 2000 → rejected")
    void testMinimumPurchase_notMet() {
        SubscriptionCoupon coupon = createPercentageCoupon("MIN", 10, null, new BigDecimal("2000"), 100);
        when(couponRepository.findByCode("MIN")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("MIN", new BigDecimal("1500"));

        assertFalse(result.getValid());
        assertTrue(result.getMessage().contains("Minimum purchase"));
    }

    @Test
    @DisplayName("Minimum purchase evaluated against ORIGINAL price, not discounted amount")
    void testMinimumPurchase_againstOriginalPrice() {
        // Plan price = 2699, min purchase = 2500, 10% discount = 269.90
        // Eligible because 2699 >= 2500 (original price), NOT 2429.10 >= 2500 (discounted)
        SubscriptionCoupon coupon = createPercentageCoupon("MINEDGE", 10, null, new BigDecimal("2500"), 100);
        when(couponRepository.findByCode("MINEDGE")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("MINEDGE", new BigDecimal("2699"));

        assertTrue(result.getValid());
        // If it were evaluated against discounted (2429.10 < 2500), it would be rejected
        assertEquals(new BigDecimal("269.90"), result.getDiscountAmount());
    }

    // ===== Usage Limit Tests =====

    @Test
    @DisplayName("Usage limit not reached: 5 used out of 10 → eligible")
    void testUsageLimit_notReached() {
        SubscriptionCoupon coupon = createPercentageCoupon("LIMIT", 10, null, null, 10);
        when(couponRepository.findByCode("LIMIT")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(5L);

        CouponValidationResponse result = couponService.validateCoupon("LIMIT", new BigDecimal("1000"));

        assertTrue(result.getValid());
    }

    @Test
    @DisplayName("Usage limit reached: 10 used out of 10 → rejected")
    void testUsageLimit_reached() {
        SubscriptionCoupon coupon = createPercentageCoupon("LIMIT", 10, null, null, 10);
        when(couponRepository.findByCode("LIMIT")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(10L);

        CouponValidationResponse result = couponService.validateCoupon("LIMIT", new BigDecimal("1000"));

        assertFalse(result.getValid());
        assertTrue(result.getMessage().contains("usage limit"));
    }

    @Test
    @DisplayName("Usage limit exceeded: 11 used out of 10 → rejected")
    void testUsageLimit_exceeded() {
        SubscriptionCoupon coupon = createPercentageCoupon("LIMIT", 10, null, null, 10);
        when(couponRepository.findByCode("LIMIT")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(11L);

        CouponValidationResponse result = couponService.validateCoupon("LIMIT", new BigDecimal("1000"));

        assertFalse(result.getValid());
    }

    // ===== Expiry Tests =====

    @Test
    @DisplayName("Expired coupon → rejected")
    void testExpiredCoupon() {
        SubscriptionCoupon coupon = createPercentageCoupon("EXPIRED", 10, null, null, 100);
        coupon.setExpiryDate(LocalDate.now().minusDays(1)); // expired yesterday
        when(couponRepository.findByCode("EXPIRED")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("EXPIRED", new BigDecimal("1000"));

        assertFalse(result.getValid());
        assertEquals("This coupon has expired", result.getMessage());
    }

    @Test
    @DisplayName("Coupon expiring today → still valid (expiry is inclusive of today)")
    void testExpiringToday() {
        SubscriptionCoupon coupon = createPercentageCoupon("TODAY", 10, null, null, 100);
        coupon.setExpiryDate(LocalDate.now()); // expires today
        when(couponRepository.findByCode("TODAY")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("TODAY", new BigDecimal("1000"));

        assertTrue(result.getValid());
    }

    // ===== Active/Inactive Tests =====

    @Test
    @DisplayName("Inactive coupon → rejected")
    void testInactiveCoupon() {
        SubscriptionCoupon coupon = createPercentageCoupon("INACTIVE", 10, null, null, 100);
        coupon.setActive(false);
        when(couponRepository.findByCode("INACTIVE")).thenReturn(Optional.of(coupon));

        CouponValidationResponse result = couponService.validateCoupon("INACTIVE", new BigDecimal("1000"));

        assertFalse(result.getValid());
        assertEquals("This coupon is no longer active", result.getMessage());
    }

    @Test
    @DisplayName("Non-existent coupon code → rejected")
    void testNonExistentCoupon() {
        when(couponRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        CouponValidationResponse result = couponService.validateCoupon("NOPE", new BigDecimal("1000"));

        assertFalse(result.getValid());
        assertEquals("Invalid coupon code", result.getMessage());
    }

    // ===== Case Insensitivity =====

    @Test
    @DisplayName("Coupon code is case-insensitive: 'save10' matches 'SAVE10'")
    void testCaseInsensitive() {
        SubscriptionCoupon coupon = createPercentageCoupon("SAVE10", 10, null, null, 100);
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("save10", new BigDecimal("1000"));

        assertTrue(result.getValid());
    }

    // ===== Rounding Tests =====

    @Test
    @DisplayName("Rounding: 10% off 2699 → 269.90 (exact 2 decimal places)")
    void testRounding_exact() {
        SubscriptionCoupon coupon = createPercentageCoupon("R", 10, null, null, 100);
        when(couponRepository.findByCode("R")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("R", new BigDecimal("2699"));

        assertEquals(new BigDecimal("269.90"), result.getDiscountAmount());
        assertEquals(new BigDecimal("2429.10"), result.getFinalAmount());
    }

    @Test
    @DisplayName("Rounding: 7% off 1000 → 70.00")
    void testRounding_wholeNumber() {
        SubscriptionCoupon coupon = createPercentageCoupon("R7", 7, null, null, 100);
        when(couponRepository.findByCode("R7")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("R7", new BigDecimal("1000"));

        assertEquals(new BigDecimal("70.00"), result.getDiscountAmount());
    }

    @Test
    @DisplayName("Rounding: 13% off 2699 → 350.87 (HALF_UP rounding)")
    void testRounding_halfUp() {
        SubscriptionCoupon coupon = createPercentageCoupon("R13", 13, null, null, 100);
        when(couponRepository.findByCode("R13")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponId(1L)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("R13", new BigDecimal("2699"));

        // 2699 * 13 / 100 = 350.87
        assertEquals(new BigDecimal("350.87"), result.getDiscountAmount());
        assertEquals(new BigDecimal("2348.13"), result.getFinalAmount());
    }
}
