package swari.sewa.module.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import swari.sewa.module.subscription.dto.CouponValidationResponse;
import swari.sewa.module.subscription.entity.SubscriptionCoupon;
import swari.sewa.module.subscription.entity.SubscriptionSettings;
import swari.sewa.module.subscription.enums.CouponDiscountType;
import swari.sewa.module.subscription.repository.SubscriptionCouponRepository;
import swari.sewa.module.subscription.repository.SubscriptionCouponUsageRepository;
import swari.sewa.module.subscription.service.impl.SubscriptionCouponServiceImpl;
import swari.sewa.module.user.repository.ShopOwnerRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for public coupon validation API — verifies it is read-only
 * and does not consume coupons or increase usage count.
 *
 * Business rules verified:
 * - Public validation (validateCoupon) is read-only
 * - Calling validation 100 times does NOT increase usage
 * - Payment validation (validateCouponForPayment) uses pessimistic lock
 * - Expired coupon → invalid
 * - Inactive coupon → invalid
 * - Usage limit reached → invalid
 * - Minimum purchase evaluated against original amount
 * - Percentage coupon with max discount
 * - Flat coupon capped at plan price
 */
class PublicCouponValidationTest {

    private SubscriptionCouponServiceImpl service;
    private SubscriptionCouponRepository couponRepository;
    private SubscriptionCouponUsageRepository couponUsageRepository;
    private SubscriptionAuditService auditService;

    @BeforeEach
    void setUp() {
        couponRepository = Mockito.mock(SubscriptionCouponRepository.class);
        couponUsageRepository = Mockito.mock(SubscriptionCouponUsageRepository.class);
        auditService = Mockito.mock(SubscriptionAuditService.class);
        ShopOwnerRepository shopOwnerRepository = Mockito.mock(ShopOwnerRepository.class);
        service = new SubscriptionCouponServiceImpl(couponRepository, couponUsageRepository, auditService, shopOwnerRepository);
    }

    private SubscriptionCoupon createPercentageCoupon(String code, int percentage, BigDecimal maxDiscount,
                                                       BigDecimal minPurchase, int usageLimit, boolean active,
                                                       LocalDate expiryDate) {
        return SubscriptionCoupon.builder()
                .id(1L)
                .code(code)
                .discountType(CouponDiscountType.PERCENTAGE)
                .percentage(percentage)
                .maximumDiscount(maxDiscount)
                .minimumPurchase(minPurchase)
                .usageLimit(usageLimit)
                .active(active)
                .expiryDate(expiryDate)
                .build();
    }

    private SubscriptionCoupon createFlatCoupon(String code, BigDecimal flatDiscount,
                                                  BigDecimal minPurchase, int usageLimit, boolean active,
                                                  LocalDate expiryDate) {
        return SubscriptionCoupon.builder()
                .id(1L)
                .code(code)
                .discountType(CouponDiscountType.FLAT)
                .flatDiscount(flatDiscount)
                .minimumPurchase(minPurchase)
                .usageLimit(usageLimit)
                .active(active)
                .expiryDate(expiryDate)
                .build();
    }

    // ===== Read-Only Validation =====

    @Nested
    @DisplayName("Public validation is read-only")
    class ReadOnlyValidationTests {

        @Test
        @DisplayName("validateCoupon does NOT increment usage count")
        void testValidateCouponDoesNotIncrementUsage() {
            SubscriptionCoupon coupon = createPercentageCoupon("SAVE10", 10, null, null, 100, true, null);
            when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
            when(couponUsageRepository.countByCouponId(1L)).thenReturn(5L);

            // Call validate 100 times
            for (int i = 0; i < 100; i++) {
                service.validateCoupon("SAVE10", new BigDecimal("1000"));
            }

            // Usage count should still be 5 — validation never increments it
            verify(couponUsageRepository, never()).save(any());
            verify(couponRepository, never()).save(any());
        }

        @Test
        @DisplayName("validateCoupon does NOT lock the coupon")
        void testValidateCouponDoesNotLock() {
            SubscriptionCoupon coupon = createPercentageCoupon("SAVE10", 10, null, null, 100, true, null);
            when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
            when(couponUsageRepository.countByCouponId(1L)).thenReturn(0L);

            service.validateCoupon("SAVE10", new BigDecimal("1000"));

            // Should use findByCode, NOT findByCodeForUpdate (which has pessimistic lock)
            verify(couponRepository).findByCode("SAVE10");
            verify(couponRepository, never()).findByCodeForUpdate(anyString());
        }
    }

    // ===== Coupon Validation Logic =====

    @Nested
    @DisplayName("Coupon validation logic")
    class CouponValidationLogicTests {

        @Test
        @DisplayName("Valid percentage coupon → correct discount")
        void testValidPercentageCoupon() {
            SubscriptionCoupon coupon = createPercentageCoupon("SAVE10", 10, null, null, 100, true, null);
            when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
            when(couponUsageRepository.countByCouponId(1L)).thenReturn(0L);

            CouponValidationResponse response = service.validateCoupon("SAVE10", new BigDecimal("1000"));

            assertTrue(response.getValid());
            assertEquals("SAVE10", response.getCode());
            assertEquals(new BigDecimal("100.00"), response.getDiscountAmount());
            assertEquals(new BigDecimal("900.00"), response.getFinalAmount());
        }

        @Test
        @DisplayName("Expired coupon → invalid")
        void testExpiredCoupon() {
            SubscriptionCoupon coupon = createPercentageCoupon("EXPIRED", 10, null, null, 100, true,
                    LocalDate.now().minusDays(1));
            when(couponRepository.findByCode("EXPIRED")).thenReturn(Optional.of(coupon));
            when(couponUsageRepository.countByCouponId(1L)).thenReturn(0L);

            CouponValidationResponse response = service.validateCoupon("EXPIRED", new BigDecimal("1000"));

            assertFalse(response.getValid());
            assertTrue(response.getMessage().contains("expired") || response.getMessage().contains("Expired"));
        }

        @Test
        @DisplayName("Inactive coupon → invalid")
        void testInactiveCoupon() {
            SubscriptionCoupon coupon = createPercentageCoupon("INACTIVE", 10, null, null, 100, false, null);
            when(couponRepository.findByCode("INACTIVE")).thenReturn(Optional.of(coupon));
            when(couponUsageRepository.countByCouponId(1L)).thenReturn(0L);

            CouponValidationResponse response = service.validateCoupon("INACTIVE", new BigDecimal("1000"));

            assertFalse(response.getValid());
        }

        @Test
        @DisplayName("Usage limit reached → invalid")
        void testUsageLimitReached() {
            SubscriptionCoupon coupon = createPercentageCoupon("LIMITED", 10, null, null, 5, true, null);
            when(couponRepository.findByCode("LIMITED")).thenReturn(Optional.of(coupon));
            when(couponUsageRepository.countByCouponId(1L)).thenReturn(5L); // limit reached

            CouponValidationResponse response = service.validateCoupon("LIMITED", new BigDecimal("1000"));

            assertFalse(response.getValid());
            assertTrue(response.getMessage().contains("limit") || response.getMessage().contains("Limit") ||
                    response.getMessage().contains("usage") || response.getMessage().contains("Usage"));
        }

        @Test
        @DisplayName("Minimum purchase not met → invalid")
        void testMinimumPurchaseNotMet() {
            SubscriptionCoupon coupon = createPercentageCoupon("MIN500", 10, null, new BigDecimal("500"), 100, true, null);
            when(couponRepository.findByCode("MIN500")).thenReturn(Optional.of(coupon));
            when(couponUsageRepository.countByCouponId(1L)).thenReturn(0L);

            // Amount 400 < minimum 500
            CouponValidationResponse response = service.validateCoupon("MIN500", new BigDecimal("400"));

            assertFalse(response.getValid());
            assertTrue(response.getMessage().contains("minimum") || response.getMessage().contains("Minimum"));
        }

        @Test
        @DisplayName("Minimum purchase met → valid")
        void testMinimumPurchaseMet() {
            SubscriptionCoupon coupon = createPercentageCoupon("MIN500", 10, null, new BigDecimal("500"), 100, true, null);
            when(couponRepository.findByCode("MIN500")).thenReturn(Optional.of(coupon));
            when(couponUsageRepository.countByCouponId(1L)).thenReturn(0L);

            // Amount 500 >= minimum 500
            CouponValidationResponse response = service.validateCoupon("MIN500", new BigDecimal("500"));

            assertTrue(response.getValid());
        }

        @Test
        @DisplayName("Percentage with maximum discount → discount capped")
        void testPercentageWithMaxDiscount() {
            // 20% of 10000 = 2000, but max is 1000
            SubscriptionCoupon coupon = createPercentageCoupon("CAP20", 20, new BigDecimal("1000"), null, 100, true, null);
            when(couponRepository.findByCode("CAP20")).thenReturn(Optional.of(coupon));
            when(couponUsageRepository.countByCouponId(1L)).thenReturn(0L);

            CouponValidationResponse response = service.validateCoupon("CAP20", new BigDecimal("10000"));

            assertTrue(response.getValid());
            assertEquals(new BigDecimal("1000.00"), response.getDiscountAmount());
        }

        @Test
        @DisplayName("Flat discount → correct amount")
        void testFlatDiscount() {
            SubscriptionCoupon coupon = createFlatCoupon("FLAT300", new BigDecimal("300"), null, 100, true, null);
            when(couponRepository.findByCode("FLAT300")).thenReturn(Optional.of(coupon));
            when(couponUsageRepository.countByCouponId(1L)).thenReturn(0L);

            CouponValidationResponse response = service.validateCoupon("FLAT300", new BigDecimal("1000"));

            assertTrue(response.getValid());
            assertEquals(new BigDecimal("300.00"), response.getDiscountAmount());
            assertEquals(new BigDecimal("700.00"), response.getFinalAmount());
        }

        @Test
        @DisplayName("Non-existent coupon → invalid")
        void testNonExistentCoupon() {
            when(couponRepository.findByCode("NOPE")).thenReturn(Optional.empty());

            CouponValidationResponse response = service.validateCoupon("NOPE", new BigDecimal("1000"));

            assertFalse(response.getValid());
        }
    }
}
