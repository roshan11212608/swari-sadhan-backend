package swari.sewa.module.subscription.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.enums.PlanCategory;
import swari.sewa.module.subscription.enums.PlanStatus;
import swari.sewa.module.subscription.enums.PlanVisibility;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query("SELECT p FROM SubscriptionPlan p WHERE " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:visibility IS NULL OR p.visibility = :visibility) " +
           "AND (:category IS NULL OR p.category = :category)")
    Page<SubscriptionPlan> findWithFilters(
            @Param("search") String search,
            @Param("status") PlanStatus status,
            @Param("visibility") PlanVisibility visibility,
            @Param("category") PlanCategory category,
            Pageable pageable);

    @Query("SELECT p FROM SubscriptionPlan p LEFT JOIN FETCH p.pricings LEFT JOIN FETCH p.restrictions LEFT JOIN FETCH p.features WHERE p.id = :id")
    Optional<SubscriptionPlan> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan.id = :planId AND s.status IN ('ACTIVE', 'TRIAL')")
    Long countActiveSubscriptionsByPlanId(@Param("planId") Long planId);

    long countByStatus(PlanStatus status);
    List<SubscriptionPlan> findByStatusOrderBySortOrderAsc(PlanStatus status);

    Optional<SubscriptionPlan> findFirstByStatus(PlanStatus status);
}
