package swari.sewa.module.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swari.sewa.module.subscription.entity.SubscriptionPlanPricing;

@Repository
public interface SubscriptionPlanPricingRepository extends JpaRepository<SubscriptionPlanPricing, Long> {
}
