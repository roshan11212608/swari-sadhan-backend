package swari.sewa.module.subscription.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swari.sewa.module.subscription.dto.*;


import java.math.BigDecimal;
import java.util.List;

public interface SubscriptionCouponService {
    Page<CouponResponse> getCoupons(String search, Boolean active, Pageable pageable);
    CouponResponse getCouponById(Long id);
    CouponResponse createCoupon(CreateCouponRequest request, Long adminUserId);
    CouponResponse updateCoupon(Long id, UpdateCouponRequest request, Long adminUserId);
    void deleteCoupon(Long id, Long adminUserId);
    CouponResponse toggleCoupon(Long id, Long adminUserId);

    /**
     * Validate a coupon for a given amount (read-only, for UI preview).
     * Does NOT lock the coupon or consume usage.
     */
    List<CouponUsageResponse> getCouponUsages(Long couponId);

    CouponValidationResponse validateCoupon(String code, BigDecimal amount);

    /**
     * Validate a coupon for a given amount with a pessimistic write lock
     * on the coupon row and usage count. Used during payment creation
     * to prevent concurrent requests from exceeding the usage limit.
     *
     * The caller must be inside a transaction. The lock is held until
     * the transaction commits or rolls back.
     *
     * Minimum purchase is evaluated against the ORIGINAL plan price
     * (before discount), not the discounted amount.
     */
    CouponValidationResponse validateCouponForPayment(String code, BigDecimal amount);
}
