package swari.sewa.module.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.subscription.entity.Subscription;
import swari.sewa.module.subscription.enums.SubscriptionStatus;
import swari.sewa.module.subscription.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that expires subscriptions (ACTIVE or TRIAL) whose
 * endDate has passed.
 *
 * <p>Runs every hour. The job is idempotent — expired subscriptions
 * are already filtered out by the query (status IN ACTIVE, TRIAL).
 *
 * <p>This is the single source of truth for automatic expiration.
 * No other mechanism should compete with this job.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpirationScheduler {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * Expire subscriptions every hour at 5 minutes past the hour.
     * Fixed delay backup: also run every 30 minutes after the last run.
     */
    @Scheduled(cron = "0 5 * * * *")
    @Transactional
    public void expireSubscriptions() {
        List<Subscription> expired = subscriptionRepository.findExpiredSubscriptions();
        if (expired.isEmpty()) {
            log.debug("No subscriptions to expire");
            return;
        }

        log.info("Found {} expired subscriptions to process", expired.size());
        int trialCount = 0;
        int activeCount = 0;

        for (Subscription sub : expired) {
            try {
                SubscriptionStatus oldStatus = sub.getStatus();
                sub.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(sub);

                if (oldStatus == SubscriptionStatus.TRIAL) {
                    trialCount++;
                } else {
                    activeCount++;
                }
                log.info("Expired subscription {} for shop_owner {} (was {}, ended {})",
                        sub.getId(), sub.getShopOwnerId(), oldStatus, sub.getEndDate());
            } catch (Exception e) {
                log.error("Failed to expire subscription {} for shop_owner {}: {}",
                        sub.getId(), sub.getShopOwnerId(), e.getMessage(), e);
            }
        }

        log.info("Expiration job complete: {} trials expired, {} active subscriptions expired",
                trialCount, activeCount);
    }
}
