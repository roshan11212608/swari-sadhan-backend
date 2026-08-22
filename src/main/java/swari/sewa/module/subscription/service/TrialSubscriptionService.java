package swari.sewa.module.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.subscription.entity.Subscription;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.entity.SubscriptionPlanRestriction;
import swari.sewa.module.subscription.entity.SubscriptionTrialConfig;
import swari.sewa.module.subscription.enums.PlanStatus;
import swari.sewa.module.subscription.enums.SubscriptionStatus;
import swari.sewa.module.subscription.repository.SubscriptionPlanRepository;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.repository.SubscriptionTrialConfigRepository;
import swari.sewa.module.auth.service.EmailService;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Starts a free trial subscription for a newly-approved shop owner
 * after they change their temporary password on first login.
 *
 * <p>Design notes:
 * <ul>
 *   <li>Only creates a trial if no <em>active</em> subscription (ACTIVE or TRIAL)
 *       exists for the shop owner. Expired/cancelled historical subscriptions
 *       do NOT block a new trial.</li>
 *   <li>The trial plan is configurable via {@link SubscriptionTrialConfig#trialPlanId}.
 *       Falls back to the first published plan if not set.</li>
 *   <li>Trial duration comes from {@link SubscriptionTrialConfig#duration}.</li>
 *   <li>A database unique index on (shop_owner_id WHERE status IN ACTIVE,TRIAL)
 *       provides the final concurrency safety net. If a concurrent request
 *       wins, the loser gets a DataIntegrityViolationException which we
 *       swallow (the trial was already created).</li>
 *   <li>Trial creation runs in a REQUIRES_NEW transaction so that a trial
 *       creation failure does NOT roll back the password change.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrialSubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTrialConfigRepository trialConfigRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final EmailService emailService;

    /**
     * Start a trial subscription for the given shop owner if eligible.
     *
     * @return true if a trial was started, false if skipped (already has
     *         active sub, trial inactive, etc.)
     * @throws RuntimeException if the trial should have been created but
     *         could not be (e.g. no plan available)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean startTrialIfNeeded(Long shopOwnerId) {
        // 1. Check if shop owner already has an ACTIVE or TRIAL subscription
        if (hasActiveOrTrialSubscription(shopOwnerId)) {
            log.info("Shop owner {} already has an ACTIVE/TRIAL subscription, skipping trial", shopOwnerId);
            return false;
        }

        // 2. Get trial config
        SubscriptionTrialConfig trial = trialConfigRepository.findById(1L).orElse(null);
        if (trial == null) {
            log.warn("Trial config not found, skipping trial for shop owner {}", shopOwnerId);
            return false;
        }
        if (!Boolean.TRUE.equals(trial.getActive())) {
            log.info("Trial config is inactive, skipping trial for shop owner {}", shopOwnerId);
            return false;
        }

        // 3. Resolve the trial plan
        SubscriptionPlan plan = resolveTrialPlan(trial);
        if (plan == null) {
            log.error("No plan available for trial subscription (trialPlanId={}, fallback failed) for shop owner {}",
                    trial.getTrialPlanId(), shopOwnerId);
            throw new RuntimeException("Trial plan not available");
        }

        // 4. Create the trial subscription
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(trial.getDuration());

        // Snapshot plan details so later admin changes don't affect this trial
        Integer vehicleLimit = trial.getVehicleLimit(); // trial-specific limit takes precedence
        if (vehicleLimit == null && plan.getRestrictions() != null) {
            for (SubscriptionPlanRestriction r : plan.getRestrictions()) {
                if (r.getMaxVehicles() != null) {
                    vehicleLimit = r.getMaxVehicles();
                    break;
                }
            }
        }

        Subscription subscription = Subscription.builder()
                .shopOwnerId(shopOwnerId)
                .plan(plan)
                .trialId(trial.getId())
                .startDate(now)
                .endDate(endDate)
                .renewalDate(endDate)
                .status(SubscriptionStatus.TRIAL)
                .autoRenewal(false)
                // Snapshot plan details at trial start
                .planNameSnapshot(plan.getName())
                .planDescriptionSnapshot(plan.getShortDescription() != null ? plan.getShortDescription() : plan.getDescription())
                .planIconSnapshot(plan.getIcon())
                .planThemeColorSnapshot(plan.getThemeColor())
                .vehicleLimitSnapshot(vehicleLimit)
                .billingCycleSnapshot("TRIAL")
                .build();

        try {
            subscriptionRepository.save(subscription);
            log.info("Trial subscription started for shop owner {}: {} days, plan={}, ends {}",
                    shopOwnerId, trial.getDuration(), plan.getName(), endDate);

            // Send congratulatory trial-started email
            sendTrialStartedEmail(shopOwnerId, trial, plan, now, endDate);

            return true;
        } catch (DataIntegrityViolationException e) {
            // Concurrent request already created the trial — this is fine
            log.info("Trial subscription for shop owner {} was already created by a concurrent request", shopOwnerId);
            return false;
        }
    }

    /**
     * Send a congratulatory email to the shop owner when their trial starts.
     * Email failures are logged but do not prevent the trial from activating.
     */
    private void sendTrialStartedEmail(Long shopOwnerId, SubscriptionTrialConfig trial,
                                       SubscriptionPlan plan, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            ShopOwner shopOwner = shopOwnerRepository.findById(shopOwnerId).orElse(null);
            if (shopOwner == null || shopOwner.getEmail() == null) {
                log.warn("Could not send trial email — shop owner {} not found or no email", shopOwnerId);
                return;
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
            String startDateStr = startDate.format(fmt);
            String endDateStr = endDate.format(fmt);
            String ownerName = shopOwner.getFirstName() != null ? shopOwner.getFirstName() : "Shop Owner";

            String subject = "Welcome to Swari Sadhan - Your Free Trial Has Started!";
            String htmlBody = "<div style='font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:20px'>"
                    + "<h2 style='color:#f97316'>Swari Sadhan</h2>"
                    + "<p>Dear " + ownerName + ",</p>"
                    + "<p>Congratulations! Your free trial has been successfully activated.</p>"
                    + "<div style='background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;padding:16px;margin:16px 0'>"
                    + "<p style='margin:4px 0'><strong>Trial Plan:</strong> " + plan.getName() + "</p>"
                    + "<p style='margin:4px 0'><strong>Trial Duration:</strong> " + trial.getDuration() + " days</p>"
                    + "<p style='margin:4px 0'><strong>Start Date:</strong> " + startDateStr + "</p>"
                    + "<p style='margin:4px 0'><strong>End Date:</strong> " + endDateStr + "</p>"
                    + "</div>"
                    + "<p>You now have full access to all the features of the <strong>" + plan.getName() + "</strong> plan during your trial period.</p>"
                    + "<p style='color:#dc2626;font-weight:600'>Your trial will expire on " + endDateStr + ". "
                    + "Please subscribe to a paid plan before the trial ends to continue using Swari Sadhan without interruption.</p>"
                    + "<a href='http://localhost:3000/shopowner/subscription' "
                    + "style='display:inline-block;background:#f97316;color:white;padding:10px 24px;border-radius:6px;text-decoration:none;margin:8px 0'>"
                    + "View Subscription Plans</a>"
                    + "<p style='color:#6b7280;font-size:12px;margin-top:24px'>If you have any questions, please contact our support team.</p>"
                    + "</div>";

            emailService.sendHtmlEmail(shopOwner.getEmail(), subject, htmlBody);
            log.info("Trial-started email sent to {} for shop owner {}", shopOwner.getEmail(), shopOwnerId);
        } catch (Exception e) {
            log.error("Failed to send trial-started email for shop owner {}: {}", shopOwnerId, e.getMessage(), e);
            // Don't throw — the trial is already activated, email is secondary
        }
    }

    /**
     * Check if the shop owner has any ACTIVE or TRIAL subscription.
     * EXPIRED, CANCELLED, SUSPENDED do NOT count.
     */
    private boolean hasActiveOrTrialSubscription(Long shopOwnerId) {
        List<Subscription> activeSubs = subscriptionRepository
                .findByShopOwnerIdAndStatus(shopOwnerId, SubscriptionStatus.ACTIVE);
        if (!activeSubs.isEmpty()) {
            return true;
        }
        List<Subscription> trialSubs = subscriptionRepository
                .findByShopOwnerIdAndStatus(shopOwnerId, SubscriptionStatus.TRIAL);
        return !trialSubs.isEmpty();
    }

    /**
     * Resolve the plan to use for the trial:
     * 1. If trialPlanId is set, use that plan.
     * 2. Otherwise, use the first published plan.
     */
    private SubscriptionPlan resolveTrialPlan(SubscriptionTrialConfig trial) {
        if (trial.getTrialPlanId() != null) {
            Optional<SubscriptionPlan> planOpt = subscriptionPlanRepository.findById(trial.getTrialPlanId());
            if (planOpt.isPresent()) {
                return planOpt.get();
            }
            log.warn("Configured trialPlanId {} not found, falling back to first published plan", trial.getTrialPlanId());
        }
        // Fallback: first published plan
        return subscriptionPlanRepository.findFirstByStatus(PlanStatus.PUBLISHED).orElse(null);
    }
}
