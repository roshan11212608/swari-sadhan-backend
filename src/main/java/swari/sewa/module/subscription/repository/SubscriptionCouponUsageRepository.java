package swari.sewa.module.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.subscription.entity.SubscriptionCouponUsage;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionCouponUsageRepository extends JpaRepository<SubscriptionCouponUsage, Long> {

    long countByCouponId(Long couponId);

    @Query("SELECT COUNT(u) FROM SubscriptionCouponUsage u WHERE u.couponId = :couponId")
    long countUsagesByCouponId(@Param("couponId") Long couponId);

    List<SubscriptionCouponUsage> findByCouponIdAndShopOwnerId(Long couponId, Long shopOwnerId);

    Optional<SubscriptionCouponUsage> findByCouponIdAndTransactionId(Long couponId, Long transactionId);

    /**
     * Count usages with a pessimistic write lock to prevent race conditions
     * when checking usage limits during payment creation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(u) FROM SubscriptionCouponUsage u WHERE u.couponId = :couponId")
    long countUsagesByCouponIdForUpdate(@Param("couponId") Long couponId);
}
