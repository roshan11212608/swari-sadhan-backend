package swari.sewa.module.subscription.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swari.sewa.module.subscription.dto.*;

public interface SubscriptionService {
    Page<SubscriberResponse> getSubscribers(String search, String status, Pageable pageable);
    SubscriberDetailsResponse getSubscriberById(Long id);
    SubscriberResponse upgradeSubscription(Long id, UpgradeSubscriptionRequest request, Long adminUserId);
    SubscriberResponse downgradeSubscription(Long id, DowngradeSubscriptionRequest request, Long adminUserId);
    SubscriberResponse suspendSubscription(Long id, SuspendSubscriptionRequest request, Long adminUserId);
    SubscriberResponse cancelSubscription(Long id, CancelSubscriptionRequest request, Long adminUserId);
}
