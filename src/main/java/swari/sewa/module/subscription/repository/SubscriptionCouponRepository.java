package swari.sewa.module.subscription.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.subscription.entity.SubscriptionCoupon;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface SubscriptionCouponRepository extends JpaRepository<SubscriptionCoupon, Long> {

    Optional<SubscriptionCoupon> findByCode(String code);
    boolean existsByCode(String code);

    /**
     * Find a coupon by code with a pessimistic write lock.
     * Used during payment creation to prevent concurrent requests from
     * exceeding the usage limit.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM SubscriptionCoupon c WHERE c.code = :code")
    Optional<SubscriptionCoupon> findByCodeForUpdate(@Param("code") String code);

    @Query("SELECT c FROM SubscriptionCoupon c WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:active IS NULL OR c.active = :active)")
    Page<SubscriptionCoupon> findWithFilters(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);
}
