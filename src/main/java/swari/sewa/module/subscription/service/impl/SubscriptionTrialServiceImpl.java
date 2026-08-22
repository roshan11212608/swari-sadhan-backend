package swari.sewa.module.subscription.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.subscription.dto.TrialResponse;
import swari.sewa.module.subscription.dto.UpdateTrialRequest;
import swari.sewa.module.subscription.entity.SubscriptionTrialConfig;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.enums.SubscriptionAction;
import swari.sewa.module.subscription.repository.SubscriptionPlanRepository;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.repository.SubscriptionTrialConfigRepository;
import swari.sewa.module.subscription.service.SubscriptionAuditService;
import swari.sewa.module.subscription.service.SubscriptionTrialService;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionTrialServiceImpl implements SubscriptionTrialService {

    private final SubscriptionTrialConfigRepository trialRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionAuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public TrialResponse getTrial() {
        SubscriptionTrialConfig trial = getOrCreateTrial();
        long activeUsers = subscriptionRepository.countActiveTrials();
        return mapToResponse(trial, activeUsers);
    }

    @Override
    public TrialResponse updateTrial(UpdateTrialRequest request, Long adminUserId) {
        SubscriptionTrialConfig trial = getOrCreateTrial();

        trial.setName(request.getName());
        if (request.getDescription() != null) trial.setDescription(request.getDescription());
        if (request.getDuration() != null) trial.setDuration(request.getDuration());
        // vehicleLimit can be set to null (fall back to plan restriction)
        trial.setVehicleLimit(request.getVehicleLimit());
        if (request.getActive() != null) trial.setActive(request.getActive());
        if (request.getTrialPlanId() != null) {
            // Validate that the plan exists and is published
            SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getTrialPlanId())
                    .orElseThrow(() -> new RuntimeException("Trial plan not found with id: " + request.getTrialPlanId()));
            trial.setTrialPlanId(plan.getId());
        }

        trial = trialRepository.save(trial);
        long activeUsers = subscriptionRepository.countActiveTrials();

        auditService.recordActivity(SubscriptionAction.TRIAL_UPDATED, "TRIAL", trial.getId(), adminUserId, "Trial configuration updated");

        log.info("Trial configuration updated by admin {}", adminUserId);
        return mapToResponse(trial, activeUsers);
    }

    private SubscriptionTrialConfig getOrCreateTrial() {
        return trialRepository.findById(1L).orElseGet(() -> {
            SubscriptionTrialConfig defaults = SubscriptionTrialConfig.builder()
                    .id(1L)
                    .name("Newcomer Free Trial")
                    .description("Free trial for new shop owners to explore the platform")
                    .duration(14)
                    .eligibilityRules("New shop owners only (first-time registration)")
                    .active(true)
                    .build();
            log.info("Creating default trial configuration");
            return trialRepository.save(defaults);
        });
    }

    private TrialResponse mapToResponse(SubscriptionTrialConfig trial, long activeUsers) {
        String trialPlanName = null;
        if (trial.getTrialPlanId() != null) {
            trialPlanName = subscriptionPlanRepository.findById(trial.getTrialPlanId())
                    .map(SubscriptionPlan::getName)
                    .orElse(null);
        }
        return TrialResponse.builder()
                .id(trial.getId())
                .name(trial.getName())
                .description(trial.getDescription())
                .duration(trial.getDuration())
                .vehicleLimit(trial.getVehicleLimit())
                .eligibilityRules(trial.getEligibilityRules())
                .trialPlanId(trial.getTrialPlanId())
                .trialPlanName(trialPlanName)
                .active(trial.getActive())
                .activeUsers(activeUsers)
                .createdDate(trial.getCreatedAt())
                .updatedDate(trial.getUpdatedAt())
                .build();
    }
}
