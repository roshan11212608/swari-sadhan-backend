package swari.sewa.module.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.subscription.dto.SubscriptionPlanResponse;
import swari.sewa.module.subscription.service.SubscriptionCouponService;
import swari.sewa.module.subscription.service.SubscriptionPlanService;
import swari.sewa.module.subscription.service.SubscriptionSettingsService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Public-facing subscription plan endpoints for shop owners.
 * Only returns PUBLISHED plans — no authentication required to browse.
 */
@RestController
@RequestMapping("/api/subscription/plans")
@RequiredArgsConstructor
public class PublicSubscriptionPlanController {

    private final SubscriptionPlanService planService;
    private final SubscriptionSettingsService settingsService;
    private final SubscriptionCouponService couponService;

    /**
     * Get all published plans visible to shop owners.
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getPublishedPlans() {
        List<SubscriptionPlanResponse> plans = planService.getPublishedPlans();
        return ResponseEntity.ok(ApiResponse.success(plans, "Published plans retrieved successfully"));
    }

    /**
     * Get public tax/VAT settings for price display on the shop owner payment page.
     */
    @GetMapping("/tax-settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTaxSettings() {
        var settings = settingsService.getSettingsEntity();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "enableVat", Boolean.TRUE.equals(settings.getEnableVat()),
                "taxPercentage", settings.getTaxPercentage() != null ? settings.getTaxPercentage() : 0,
                "currency", settings.getCurrency() != null ? settings.getCurrency() : "NPR"
        ), "Tax settings retrieved"));
    }

    /**
     * Validate a coupon code for a given amount. Shop owners call this before payment.
     */
    @GetMapping("/coupon/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal amount) {
        var result = couponService.validateCoupon(code, amount);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "valid", result.getValid(),
                "message", result.getMessage(),
                "code", result.getCode() != null ? result.getCode() : code,
                "discountType", result.getDiscountType() != null ? result.getDiscountType() : "",
                "discountAmount", result.getDiscountAmount() != null ? result.getDiscountAmount() : BigDecimal.ZERO,
                "originalAmount", result.getOriginalAmount(),
                "finalAmount", result.getFinalAmount(),
                "couponId", result.getCouponId() != null ? result.getCouponId() : 0
        ), result.getMessage()));
    }
}
