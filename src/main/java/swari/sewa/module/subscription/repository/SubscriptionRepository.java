package swari.sewa.module.subscription.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.subscription.entity.Subscription;
import swari.sewa.module.subscription.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("SELECT s FROM Subscription s WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "EXISTS (SELECT 1 FROM ShopOwner so WHERE so.id = s.shopOwnerId AND " +
           "(LOWER(so.shopName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(so.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(so.lastName) LIKE LOWER(CONCAT('%', :search, '%'))))) " +
           "AND (:status IS NULL OR s.status = :status)")
    Page<Subscription> findWithFilters(@Param("search") String search, @Param("status") SubscriptionStatus status, Pageable pageable);

    @Query("SELECT s FROM Subscription s LEFT JOIN FETCH s.plan WHERE s.id = :id")
    Optional<Subscription> findByIdWithPlan(@Param("id") Long id);

    List<Subscription> findByShopOwnerId(Long shopOwnerId);
    List<Subscription> findByShopOwnerIdAndStatus(Long shopOwnerId, SubscriptionStatus status);

    Optional<Subscription> findFirstByShopOwnerIdOrderByCreatedAtDesc(Long shopOwnerId);

    @Query("SELECT s FROM Subscription s WHERE s.shopOwnerId = :shopOwnerId AND s.status = swari.sewa.module.subscription.enums.SubscriptionStatus.TRIAL")
    Optional<Subscription> findTrialByShopOwnerId(@Param("shopOwnerId") Long shopOwnerId);

    long countByStatus(SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.status = swari.sewa.module.subscription.enums.SubscriptionStatus.TRIAL ORDER BY s.createdAt DESC")
    List<Subscription> findAllTrials();

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = swari.sewa.module.subscription.enums.SubscriptionStatus.TRIAL AND s.endDate > CURRENT_TIMESTAMP")
    long countActiveTrials();

    @Query("SELECT s FROM Subscription s WHERE s.status IN (swari.sewa.module.subscription.enums.SubscriptionStatus.ACTIVE, swari.sewa.module.subscription.enums.SubscriptionStatus.TRIAL) AND s.endDate BETWEEN CURRENT_TIMESTAMP AND :endDate")
    List<Subscription> findExpiringSoon(@Param("endDate") LocalDateTime endDate);

    /**
     * Find all ACTIVE or TRIAL subscriptions whose endDate has passed.
     * Used by the scheduled expiration job to mark them EXPIRED.
     */
    @Query("SELECT s FROM Subscription s WHERE s.status IN (swari.sewa.module.subscription.enums.SubscriptionStatus.ACTIVE, swari.sewa.module.subscription.enums.SubscriptionStatus.TRIAL) AND s.endDate < CURRENT_TIMESTAMP")
    List<Subscription> findExpiredSubscriptions();

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status IN (swari.sewa.module.subscription.enums.SubscriptionStatus.ACTIVE, swari.sewa.module.subscription.enums.SubscriptionStatus.TRIAL)")
    long countActiveSubscriptions();

    @Query("SELECT s.plan.id, COUNT(s) FROM Subscription s WHERE s.status IN (swari.sewa.module.subscription.enums.SubscriptionStatus.ACTIVE, swari.sewa.module.subscription.enums.SubscriptionStatus.TRIAL) GROUP BY s.plan.id")
    List<Object[]> countSubscribersByPlan();

    /**
     * Raw inputs for MRR: for each ACTIVE (paid, non-trial) subscription return
     * the price actually paid and the billing cycle it was purchased on. The
     * caller normalises each row to a monthly figure. TRIAL subscriptions are
     * excluded because they contribute no recurring revenue.
     *
     * Returns rows of: billingCycleSnapshot, summedPricePaid
     */
    @Query("SELECT s.billingCycleSnapshot, COALESCE(SUM(s.pricePaid), 0) " +
           "FROM Subscription s " +
           "WHERE s.status = swari.sewa.module.subscription.enums.SubscriptionStatus.ACTIVE " +
           "AND s.pricePaid IS NOT NULL " +
           "GROUP BY s.billingCycleSnapshot")
    List<Object[]> sumActivePricePaidByBillingCycle();

    /**
     * Find expired trial subscriptions within the last N days where the shop owner
     * has NOT purchased a paid (ACTIVE) subscription afterward.
     */
    @Query("SELECT s FROM Subscription s WHERE " +
           "s.status = swari.sewa.module.subscription.enums.SubscriptionStatus.EXPIRED " +
           "AND s.trialId IS NOT NULL " +
           "AND s.endDate >= :since " +
           "AND NOT EXISTS (" +
           "  SELECT 1 FROM Subscription s2 WHERE s2.shopOwnerId = s.shopOwnerId " +
           "  AND s2.status = swari.sewa.module.subscription.enums.SubscriptionStatus.ACTIVE" +
           ") " +
           "ORDER BY s.endDate DESC")
    List<Subscription> findRecentlyExpiredTrialsWithoutSubscription(@Param("since") LocalDateTime since);

    @Query("SELECT FUNCTION('DATE_FORMAT', s.startDate, '%Y-%m') as month, " +
           "COUNT(s) " +
           "FROM Subscription s " +
           "WHERE s.status IN (swari.sewa.module.subscription.enums.SubscriptionStatus.ACTIVE, swari.sewa.module.subscription.enums.SubscriptionStatus.TRIAL) " +
           "AND s.startDate <= FUNCTION('LAST_DAY', :endDate) " +
           "AND (s.endDate >= FUNCTION('DATE', CONCAT(FUNCTION('DATE_FORMAT', s.startDate, '%Y-%m'), '-01')) OR s.endDate IS NULL) " +
           "GROUP BY FUNCTION('DATE_FORMAT', s.startDate, '%Y-%m') ORDER BY month")
    List<Object[]> getActiveSubscribersTrendByMonth(@Param("endDate") LocalDateTime endDate);
}
