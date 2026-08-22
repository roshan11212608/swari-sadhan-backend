package swari.sewa.module.subscription.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swari.sewa.module.subscription.dto.RecentActivityResponse;
import swari.sewa.module.subscription.enums.SubscriptionAction;

public interface SubscriptionAuditService {
    void recordActivity(SubscriptionAction action, String entityType, Long entityId, Long adminUserId, String description);
    Page<RecentActivityResponse> getRecentActivities(Pageable pageable);
}
