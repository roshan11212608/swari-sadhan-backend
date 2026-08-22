package swari.sewa.module.subscription.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.subscription.dto.RecentActivityResponse;
import swari.sewa.module.subscription.entity.SubscriptionActivity;
import swari.sewa.module.subscription.enums.SubscriptionAction;
import swari.sewa.module.subscription.repository.SubscriptionActivityRepository;
import swari.sewa.module.subscription.service.SubscriptionAuditService;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionAuditServiceImpl implements SubscriptionAuditService {

    private final SubscriptionActivityRepository activityRepository;

    @Override
    public void recordActivity(SubscriptionAction action, String entityType, Long entityId, Long adminUserId, String description) {
        SubscriptionActivity activity = SubscriptionActivity.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .adminUserId(adminUserId)
                .description(description)
                .status("COMPLETED")
                .build();
        activityRepository.save(activity);
        log.info("Subscription activity recorded: {} for {} #{} by admin {}", action, entityType, entityId, adminUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecentActivityResponse> getRecentActivities(Pageable pageable) {
        return activityRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToResponse);
    }

    private RecentActivityResponse mapToResponse(SubscriptionActivity activity) {
        return RecentActivityResponse.builder()
                .id(activity.getId())
                .action(activity.getAction() != null ? activity.getAction().name() : null)
                .entityType(activity.getEntityType())
                .entityId(activity.getEntityId())
                .adminUserId(activity.getAdminUserId())
                .description(activity.getDescription())
                .status(activity.getStatus())
                .createdDate(activity.getCreatedAt())
                .build();
    }
}
