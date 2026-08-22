package swari.sewa.module.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.subscription.entity.SubscriptionCouponUsage;
import swari.sewa.module.subscription.repository.SubscriptionCouponUsageRepository;

import java.math.BigDecimal;

/**
 * Records coupon usage in a separate transaction to prevent JPA session corruption
 * when a unique constraint violation occurs (e.g. duplicate callback).
 *
 * Uses REQUIRES_NEW propagation with only primitive/serializable parameters
 * (not JPA entities) to avoid session sharing issues.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponUsageRecorder {

    private final SubscriptionCouponUsageRepository couponUsageRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUsage(Long couponId, Long paymentId, Long shopOwnerId, BigDecimal discountAmount) {
        if (couponId == null || paymentId == null) {
            return;
        }
        try {
            // Idempotency: check if usage already recorded for this (coupon_id, transaction_id).
            var existing = couponUsageRepository.findByCouponIdAndTransactionId(couponId, paymentId);
            if (existing.isPresent()) {
                log.info("Coupon usage already recorded for coupon={}, payment={} — skipping (idempotent)",
                        couponId, paymentId);
                return;
            }

            var usage = SubscriptionCouponUsage.builder()
                    .couponId(couponId)
                    .transactionId(paymentId)
                    .shopOwnerId(shopOwnerId)
                    .discountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO)
                    .build();
            couponUsageRepository.save(usage);
            log.info("Coupon usage recorded: couponId={}, shopOwner={}, discount={}, paymentId={}",
                    couponId, shopOwnerId, discountAmount, paymentId);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Unique constraint violation — another thread/callback already recorded this usage
            log.info("Coupon usage already recorded (constraint violation) for coupon={}, payment={} — skipping",
                    couponId, paymentId);
        } catch (Exception e) {
            log.error("Failed to record coupon usage for payment {}: {}", paymentId, e.getMessage());
        }
    }
}
