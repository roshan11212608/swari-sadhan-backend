package swari.sewa.module.subscription.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.subscription.dto.CouponResponse;
import swari.sewa.module.subscription.dto.CouponUsageResponse;
import swari.sewa.module.subscription.dto.CouponValidationResponse;
import swari.sewa.module.subscription.dto.CreateCouponRequest;
import swari.sewa.module.subscription.dto.UpdateCouponRequest;
import swari.sewa.module.subscription.entity.SubscriptionCoupon;
import swari.sewa.module.subscription.enums.CouponDiscountType;
import swari.sewa.module.subscription.enums.SubscriptionAction;
import swari.sewa.module.subscription.exception.CouponNotFoundException;
import swari.sewa.module.subscription.exception.DuplicateCouponCodeException;
import swari.sewa.module.subscription.exception.InvalidCouponException;
import swari.sewa.module.subscription.repository.SubscriptionCouponRepository;
import swari.sewa.module.subscription.repository.SubscriptionCouponUsageRepository;
import swari.sewa.module.subscription.service.SubscriptionAuditService;
import swari.sewa.module.subscription.service.SubscriptionCouponService;

import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionCouponServiceImpl implements SubscriptionCouponService {

    private final SubscriptionCouponRepository couponRepository;
    private final SubscriptionCouponUsageRepository couponUsageRepository;
    private final SubscriptionAuditService auditService;
    private final ShopOwnerRepository shopOwnerRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CouponResponse> getCoupons(String search, Boolean active, Pageable pageable) {
        log.info("Fetching coupons with search: '{}', active: {}", search, active);
        return couponRepository.findWithFilters(search, active, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCouponById(Long id) {
        log.info("Fetching coupon by id: {}", id);
        SubscriptionCoupon coupon = findCouponById(id);
        return mapToResponse(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponUsageResponse> getCouponUsages(Long couponId) {
        log.info("Fetching usages for coupon: {}", couponId);
        findCouponById(couponId); // validate exists
        return couponUsageRepository.findByCouponIdOrderByUsedAtDesc(couponId).stream()
                .map(this::mapToUsageResponse)
                .collect(Collectors.toList());
    }

    private CouponUsageResponse mapToUsageResponse(swari.sewa.module.subscription.entity.SubscriptionCouponUsage usage) {
        ShopOwner owner = shopOwnerRepository.findById(usage.getShopOwnerId()).orElse(null);
        String name = owner != null
                ? (owner.getFirstName() != null ? owner.getFirstName() : "") +
                  (owner.getLastName() != null && !owner.getLastName().isBlank() ? " " + owner.getLastName() : "")
                : "Unknown";
        return CouponUsageResponse.builder()
                .id(usage.getId())
                .couponId(usage.getCouponId())
                .transactionId(usage.getTransactionId())
                .shopOwnerId(usage.getShopOwnerId())
                .shopOwnerName(name.trim().isEmpty() ? "Unknown" : name)
                .shopOwnerEmail(owner != null ? owner.getEmail() : null)
                .shopOwnerPhone(owner != null ? owner.getPhone() : null)
                .discountAmount(usage.getDiscountAmount())
                .usedAt(usage.getUsedAt())
                .build();
    }

    @Override
    public CouponResponse createCoupon(CreateCouponRequest request, Long adminUserId) {
        log.info("Creating coupon with code: {} by admin {}", request.getCode(), adminUserId);

        String normalizedCode = normalizeCode(request.getCode());

        if (couponRepository.existsByCode(normalizedCode)) {
            throw new DuplicateCouponCodeException("Coupon code already exists: " + normalizedCode);
        }

        CouponDiscountType discountType = parseDiscountType(request.getDiscountType());
        validateDiscountFields(discountType, request.getPercentage(), request.getFlatDiscount());

        SubscriptionCoupon coupon = SubscriptionCoupon.builder()
                .code(normalizedCode)
                .discountType(discountType)
                .percentage(request.getPercentage())
                .flatDiscount(request.getFlatDiscount())
                .maximumDiscount(request.getMaximumDiscount())
                .minimumPurchase(request.getMinimumPurchase())
                .usageLimit(request.getUsageLimit() != null ? request.getUsageLimit() : 100)
                .expiryDate(request.getExpiryDate())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        coupon = couponRepository.save(coupon);
        auditService.recordActivity(SubscriptionAction.COUPON_CREATED, "COUPON", coupon.getId(), adminUserId,
                "Coupon created with code: " + coupon.getCode());

        log.info("Coupon created successfully with id: {}", coupon.getId());
        return mapToResponse(coupon);
    }

    @Override
    public CouponResponse updateCoupon(Long id, UpdateCouponRequest request, Long adminUserId) {
        log.info("Updating coupon id: {} by admin {}", id, adminUserId);

        SubscriptionCoupon coupon = findCouponById(id);

        if (request.getCode() != null && !request.getCode().isBlank()) {
            String normalizedCode = normalizeCode(request.getCode());
            if (!normalizedCode.equals(coupon.getCode()) && couponRepository.existsByCode(normalizedCode)) {
                throw new DuplicateCouponCodeException("Coupon code already exists: " + normalizedCode);
            }
            coupon.setCode(normalizedCode);
        }

        CouponDiscountType discountType = parseDiscountType(request.getDiscountType());
        validateDiscountFields(discountType, request.getPercentage(), request.getFlatDiscount());
        coupon.setDiscountType(discountType);

        if (request.getPercentage() != null) {
            coupon.setPercentage(request.getPercentage());
        }
        if (request.getFlatDiscount() != null) {
            coupon.setFlatDiscount(request.getFlatDiscount());
        }
        if (request.getMaximumDiscount() != null) {
            coupon.setMaximumDiscount(request.getMaximumDiscount());
        }
        if (request.getMinimumPurchase() != null) {
            coupon.setMinimumPurchase(request.getMinimumPurchase());
        }
        if (request.getUsageLimit() != null) {
            coupon.setUsageLimit(request.getUsageLimit());
        }
        if (request.getExpiryDate() != null) {
            coupon.setExpiryDate(request.getExpiryDate());
        }
        if (request.getActive() != null) {
            coupon.setActive(request.getActive());
        }

        coupon = couponRepository.save(coupon);
        auditService.recordActivity(SubscriptionAction.COUPON_UPDATED, "COUPON", coupon.getId(), adminUserId,
                "Coupon updated with code: " + coupon.getCode());

        log.info("Coupon updated successfully with id: {}", coupon.getId());
        return mapToResponse(coupon);
    }

    @Override
    public void deleteCoupon(Long id, Long adminUserId) {
        log.info("Deleting coupon id: {} by admin {}", id, adminUserId);

        SubscriptionCoupon coupon = findCouponById(id);
        String couponCode = coupon.getCode();

        couponRepository.delete(coupon);
        auditService.recordActivity(SubscriptionAction.COUPON_DELETED, "COUPON", id, adminUserId,
                "Coupon deleted with code: " + couponCode);

        log.info("Coupon deleted successfully with id: {}", id);
    }

    @Override
    public CouponResponse toggleCoupon(Long id, Long adminUserId) {
        log.info("Toggling coupon id: {} by admin {}", id, adminUserId);

        SubscriptionCoupon coupon = findCouponById(id);
        coupon.setActive(!coupon.getActive());
        coupon = couponRepository.save(coupon);

        auditService.recordActivity(SubscriptionAction.COUPON_UPDATED, "COUPON", coupon.getId(), adminUserId,
                "Coupon toggled to " + (coupon.getActive() ? "active" : "inactive") + ": " + coupon.getCode());

        log.info("Coupon id: {} toggled to {}", id, coupon.getActive());
        return mapToResponse(coupon);
    }

    private SubscriptionCoupon findCouponById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with id: " + id));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private CouponDiscountType parseDiscountType(String discountType) {
        try {
            return CouponDiscountType.valueOf(discountType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCouponException("Invalid discount type: " + discountType + ". Must be PERCENTAGE or FLAT");
        }
    }

    private void validateDiscountFields(CouponDiscountType discountType, Integer percentage, BigDecimal flatDiscount) {
        if (discountType == CouponDiscountType.PERCENTAGE) {
            if (percentage == null || percentage < 1 || percentage > 100) {
                throw new InvalidCouponException("Percentage coupon requires a percentage value between 1 and 100");
            }
        } else if (discountType == CouponDiscountType.FLAT) {
            if (flatDiscount == null || flatDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidCouponException("Flat discount coupon requires a flatDiscount greater than 0");
            }
        }
    }

    private CouponResponse mapToResponse(SubscriptionCoupon coupon) {
        long usedCount = couponUsageRepository.countByCouponId(coupon.getId());
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType() != null ? coupon.getDiscountType().name() : null)
                .percentage(coupon.getPercentage())
                .flatDiscount(coupon.getFlatDiscount())
                .maximumDiscount(coupon.getMaximumDiscount())
                .minimumPurchase(coupon.getMinimumPurchase())
                .usageLimit(coupon.getUsageLimit())
                .expiryDate(coupon.getExpiryDate())
                .active(coupon.getActive())
                .usedCount((int) usedCount)
                .createdDate(coupon.getCreatedAt())
                .updatedDate(coupon.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, BigDecimal amount) {
        return doValidateCoupon(code, amount, false);
    }

    /**
     * Locked validation for payment creation — acquires a pessimistic write lock
     * on the coupon row and usage count to prevent concurrent requests from
     * exceeding the usage limit.
     *
     * The caller (EsewaPaymentServiceImpl.createPayment) is already @Transactional,
     * so the lock is held until the payment record is saved and the transaction commits.
     */
    @Override
    @Transactional
    public CouponValidationResponse validateCouponForPayment(String code, BigDecimal amount) {
        return doValidateCoupon(code, amount, true);
    }

    /**
     * Core coupon validation logic.
     *
     * @param code   the coupon code (case-insensitive, will be normalized to UPPER)
     * @param amount the ORIGINAL plan price (before any discount)
     * @param lock   if true, acquires pessimistic write locks on the coupon and usage count
     *
     * Business rules:
     * - Minimum purchase is evaluated against the ORIGINAL plan price, NOT the discounted amount.
     *   Example: plan price = 2,699, minimum purchase = 2,000 → eligible (2,699 >= 2,000).
     * - Percentage discount: amount × percentage / 100, capped at maximumDiscount if set.
     * - Flat discount: fixed amount from flatDiscount field.
     * - Discount cannot exceed the plan price (final amount never negative).
     * - Usage limit: checked against count of subscription_coupon_usages records.
     *   When lock=true, the count uses a pessimistic write lock to prevent race conditions.
     * - Expiry: coupon.expiryDate must be today or later (null expiry = never expires).
     * - Active: coupon.active must be true.
     */
    private CouponValidationResponse doValidateCoupon(String code, BigDecimal amount, boolean lock) {
        String normalizedCode = code.trim().toUpperCase();

        SubscriptionCoupon coupon = lock
                ? couponRepository.findByCodeForUpdate(normalizedCode).orElse(null)
                : couponRepository.findByCode(normalizedCode).orElse(null);

        if (coupon == null) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .message("Invalid coupon code")
                    .code(normalizedCode)
                    .originalAmount(amount)
                    .finalAmount(amount)
                    .build();
        }

        if (!Boolean.TRUE.equals(coupon.getActive())) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .message("This coupon is no longer active")
                    .code(coupon.getCode())
                    .originalAmount(amount)
                    .finalAmount(amount)
                    .build();
        }

        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(java.time.LocalDate.now())) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .message("This coupon has expired")
                    .code(coupon.getCode())
                    .originalAmount(amount)
                    .finalAmount(amount)
                    .build();
        }

        // Check usage limit — use locked count when validating for payment
        long usedCount = lock
                ? couponUsageRepository.countUsagesByCouponIdForUpdate(coupon.getId())
                : couponUsageRepository.countByCouponId(coupon.getId());
        if (usedCount >= coupon.getUsageLimit()) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .message("This coupon has reached its usage limit")
                    .code(coupon.getCode())
                    .originalAmount(amount)
                    .finalAmount(amount)
                    .build();
        }

        // Minimum purchase is evaluated against the ORIGINAL plan price (before discount)
        if (coupon.getMinimumPurchase() != null && amount.compareTo(coupon.getMinimumPurchase()) < 0) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .message("Minimum purchase of " + coupon.getMinimumPurchase() + " required for this coupon")
                    .code(coupon.getCode())
                    .originalAmount(amount)
                    .finalAmount(amount)
                    .build();
        }

        // Calculate discount using BigDecimal (no floating-point)
        BigDecimal discountAmount;
        if (coupon.getDiscountType() == CouponDiscountType.PERCENTAGE) {
            discountAmount = amount.multiply(BigDecimal.valueOf(coupon.getPercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaximumDiscount() != null && discountAmount.compareTo(coupon.getMaximumDiscount()) > 0) {
                discountAmount = coupon.getMaximumDiscount().setScale(2, RoundingMode.HALF_UP);
            }
        } else {
            discountAmount = coupon.getFlatDiscount() != null
                    ? coupon.getFlatDiscount().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Discount cannot exceed the plan price — final amount must never be negative
        if (discountAmount.compareTo(amount) > 0) {
            discountAmount = amount.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal finalAmount = amount.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        return CouponValidationResponse.builder()
                .valid(true)
                .message("Coupon applied successfully")
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType().name())
                .percentage(coupon.getPercentage())
                .flatDiscount(coupon.getFlatDiscount())
                .discountAmount(discountAmount.setScale(2, RoundingMode.HALF_UP))
                .originalAmount(amount)
                .finalAmount(finalAmount)
                .couponId(coupon.getId())
                .build();
    }
}
