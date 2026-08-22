package swari.sewa.module.subscription.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swari.sewa.module.subscription.entity.SubscriptionActivity;

@Repository
public interface SubscriptionActivityRepository extends JpaRepository<SubscriptionActivity, Long> {
    Page<SubscriptionActivity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
