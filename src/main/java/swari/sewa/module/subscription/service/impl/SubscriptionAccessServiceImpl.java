package swari.sewa.module.subscription.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.module.subscription.dto.VehicleUsageResponse;
import swari.sewa.module.subscription.entity.Subscription;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.entity.SubscriptionPlanRestriction;
import swari.sewa.module.subscription.entity.SubscriptionTrialConfig;
import swari.sewa.module.subscription.enums.SubscriptionStatus;
import swari.sewa.module.subscription.exception.SubscriptionLimitExceededException;
import swari.sewa.module.subscription.exception.SubscriptionRequiredException;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.repository.SubscriptionTrialConfigRepository;
import swari.sewa.module.subscription.service.SubscriptionAccessService;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Centralized subscription access checks.
 *
 * <p>Vehicle limit is subscription-based: the plan's monthly vehicle limit is
 * multiplied by the billing cycle months to get the total limit for the entire
 * subscription period. For example, a plan with a monthly limit of 10 and a
 * yearly billing cycle gives a total limit of 120 vehicles for the year.
 *
 * <p>Counts vehicles added AFTER the current billing period started
 * (createdAt >= subscription.currentPeriodStart). Vehicles that existed before
 * the subscription are grandfathered and don't count against the limit.
 * Sold vehicles are still counted — selling does NOT free up a slot.
 * Selling is always allowed regardless of the add limit.
 *
 * <p>On renewal, currentPeriodStart moves to the old endDate so the new
 * period gets a fresh vehicle allowance. startDate preserves the original
 * subscription creation date for historical accuracy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionAccessServiceImpl implements SubscriptionAccessService {

    private final SubscriptionRepository subscriptionRepository;
    private final VehicleRepository vehicleRepository;
    private final SubscriptionTrialConfigRepository trialConfigRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(Long shopOwnerId) {
        List<Subscription> subs = subscriptionRepository
                .findByShopOwnerIdAndStatus(shopOwnerId, SubscriptionStatus.ACTIVE);
        return !subs.isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveTrial(Long shopOwnerId) {
        List<Subscription> subs = subscriptionRepository
                .findByShopOwnerIdAndStatus(shopOwnerId, SubscriptionStatus.TRIAL);
        return !subs.isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasVehicleAccess(Long shopOwnerId) {
        return hasActiveSubscription(shopOwnerId) || hasActiveTrial(shopOwnerId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireVehicleAccess(Long shopOwnerId) {
        if (!hasVehicleAccess(shopOwnerId)) {
            throw new SubscriptionRequiredException(
                    "Your subscription has expired. Please subscribe to a plan to continue managing vehicles.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAddVehicle(Long shopOwnerId) {
        if (!hasVehicleAccess(shopOwnerId)) {
            return false;
        }
        Subscription sub = getActiveOrTrialSubscription(shopOwnerId);
        if (sub == null) {
            return false;
        }
        // Use snapshotted vehicle limit (frozen at purchase time)
        Integer maxVehicles = sub.getVehicleLimitSnapshot();
        if (maxVehicles == null) {
            return true; // null limit = unlimited
        }
        // Count vehicles added after the current billing period started
        long currentCount = getVehiclesSinceSubscription(shopOwnerId, sub.getCurrentPeriodStart());
        return currentCount < maxVehicles;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCanAddVehicle(Long shopOwnerId) {
        // 1. Check subscription access
        requireVehicleAccess(shopOwnerId);

        // 2. Get the subscription with snapshotted limit
        Subscription sub = getActiveOrTrialSubscription(shopOwnerId);
        if (sub == null) {
            return; // no subscription = handled by requireVehicleAccess above
        }

        // Use snapshotted vehicle limit (frozen at purchase time)
        Integer maxVehicles = sub.getVehicleLimitSnapshot();
        if (maxVehicles == null) {
            return; // null limit = unlimited
        }

        // Count vehicles added after the current billing period started
        long currentCount = getVehiclesSinceSubscription(shopOwnerId, sub.getCurrentPeriodStart());
        if (currentCount >= maxVehicles) {
            String planName = sub.getPlanNameSnapshot() != null ? sub.getPlanNameSnapshot()
                    : (sub.getPlan() != null ? sub.getPlan().getName() : null);
            String cycle = sub.getBillingCycleSnapshot() != null ? sub.getBillingCycleSnapshot() : "this cycle";
            throw new SubscriptionLimitExceededException(
                    "You have reached your vehicle limit of " + maxVehicles
                            + " for your " + cycle + " subscription. Please upgrade your plan to add more vehicles.",
                    currentCount,
                    maxVehicles,
                    planName);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleUsageResponse getVehicleUsage(Long shopOwnerId) {
        Subscription sub = getActiveOrTrialSubscription(shopOwnerId);
        long totalInventory = getInventoryVehicleCount(shopOwnerId);

        if (sub == null) {
            return VehicleUsageResponse.builder()
                    .subscriptionStatus("NONE")
                    .planName(null)
                    .currentVehicleCount(totalInventory)
                    .vehicleLimit(null)
                    .remainingSlots(null)
                    .canAddVehicle(false)
                    .hasAccess(false)
                    .trialEndDate(null)
                    .trialDaysRemaining(null)
                    .startDate(null)
                    .endDate(null)
                    .billingCycle(null)
                    .daysRemaining(null)
                    .build();
        }

        String status = sub.getStatus().name(); // ACTIVE or TRIAL
        // Use snapshotted plan name (frozen at purchase time)
        String planName = sub.getPlanNameSnapshot() != null ? sub.getPlanNameSnapshot()
                : (sub.getPlan() != null ? sub.getPlan().getName() : null);

        // Use snapshotted vehicle limit (frozen at purchase time, includes carry-forward)
        Integer vehicleLimit = sub.getVehicleLimitSnapshot();
        boolean unlimited = (vehicleLimit == null);

        // Rollover detail fields
        Integer newPlanVehicleLimit = sub.getNewPlanVehicleLimit();
        Integer carriedForward = sub.getCarriedForwardVehicleLimit();

        // Count vehicles added after the current billing period started
        long currentCount = getVehiclesSinceSubscription(shopOwnerId, sub.getCurrentPeriodStart());

        boolean canAdd = unlimited ? true : currentCount < vehicleLimit;
        Integer remaining = unlimited ? null : Math.max(0, vehicleLimit - (int) currentCount);

        String trialEndDate = null;
        Integer trialDaysRemaining = null;
        if (sub.getStatus() == SubscriptionStatus.TRIAL && sub.getEndDate() != null) {
            trialEndDate = sub.getEndDate().toString();
            long days = ChronoUnit.DAYS.between(LocalDateTime.now(), sub.getEndDate());
            trialDaysRemaining = (int) Math.max(0, days);
        }

        // Common date fields for both ACTIVE and TRIAL
        // startDate = original subscription start (for history)
        // currentPeriodStart = start of current billing period (for display)
        String startDate = sub.getCurrentPeriodStart() != null
                ? sub.getCurrentPeriodStart().toString()
                : (sub.getStartDate() != null ? sub.getStartDate().toString() : null);
        String endDate = sub.getEndDate() != null ? sub.getEndDate().toString() : null;
        String billingCycle = sub.getBillingCycleSnapshot() != null ? sub.getBillingCycleSnapshot() : null;
        Integer daysRemaining = null;
        if (sub.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDateTime.now(), sub.getEndDate());
            daysRemaining = (int) Math.max(0, days);
        }

        return VehicleUsageResponse.builder()
                .subscriptionStatus(status)
                .planName(planName)
                .currentVehicleCount(currentCount)
                .vehicleLimit(vehicleLimit)
                .remainingSlots(remaining)
                .canAddVehicle(canAdd)
                .hasAccess(true)
                .trialEndDate(trialEndDate)
                .trialDaysRemaining(trialDaysRemaining)
                .startDate(startDate)
                .endDate(endDate)
                .billingCycle(billingCycle)
                .daysRemaining(daysRemaining)
                .pricePaid(sub.getPricePaid())
                .newPlanVehicleLimit(newPlanVehicleLimit)
                .carriedForwardVehicleLimit(carriedForward)
                .totalVehicleLimit(vehicleLimit)
                .vehiclesUsed(currentCount)
                .vehiclesRemaining(remaining)
                .build();
    }

    // ===== Private helpers =====

    /**
     * Count all vehicles in inventory (excludes SOLD vehicles).
     * Used for display purposes (total current inventory).
     */
    private long getInventoryVehicleCount(Long shopOwnerId) {
        long total = vehicleRepository.countByShop_ShopOwner_Id(shopOwnerId);
        long sold = vehicleRepository.countByShop_ShopOwner_IdAndStatus(shopOwnerId, VehicleStatus.SOLD);
        return total - sold;
    }

    /**
     * Count vehicles added AFTER the given date (includes SOLD).
     * Used with currentPeriodStart to count vehicles against the current
     * billing period's allowance. Vehicles that existed before the
     * subscription are grandfathered and don't count against the limit.
     *
     * The vehicle limit is the plan's monthly limit × billing cycle months.
     * Example: monthly limit 10 + yearly cycle → total limit 120.
     * Selling a vehicle does NOT free up a slot — once the total limit is
     * reached, no more can be added until the subscription renews or upgrades.
     * However, selling is always allowed regardless of the add limit.
     */
    private long getVehiclesSinceSubscription(Long shopOwnerId, LocalDateTime subscriptionStart) {
        if (subscriptionStart == null) {
            return getInventoryVehicleCount(shopOwnerId);
        }
        return vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(
                shopOwnerId, subscriptionStart);
    }

    private Optional<SubscriptionPlanRestriction> getActiveRestriction(Long shopOwnerId) {
        Subscription sub = getActiveOrTrialSubscription(shopOwnerId);
        if (sub == null || sub.getPlan() == null) {
            return Optional.empty();
        }

        // For TRIAL subscriptions, check trial config's vehicle limit first
        if (sub.getStatus() == SubscriptionStatus.TRIAL) {
            SubscriptionTrialConfig trialConfig = trialConfigRepository.findById(1L).orElse(null);
            if (trialConfig != null && trialConfig.getVehicleLimit() != null) {
                // Return a synthetic restriction with the trial config's vehicle limit
                return Optional.of(SubscriptionPlanRestriction.builder()
                        .maxVehicles(trialConfig.getVehicleLimit())
                        .build());
            }
        }

        // Fall back to plan's restriction
        SubscriptionPlan plan = sub.getPlan();
        if (plan.getRestrictions() == null || plan.getRestrictions().isEmpty()) {
            return Optional.empty();
        }
        return plan.getRestrictions().stream().findFirst();
    }

    private Subscription getActiveOrTrialSubscription(Long shopOwnerId) {
        List<Subscription> activeSubs = subscriptionRepository
                .findByShopOwnerIdAndStatus(shopOwnerId, SubscriptionStatus.ACTIVE);
        if (!activeSubs.isEmpty()) {
            return activeSubs.get(0);
        }
        List<Subscription> trialSubs = subscriptionRepository
                .findByShopOwnerIdAndStatus(shopOwnerId, SubscriptionStatus.TRIAL);
        return trialSubs.isEmpty() ? null : trialSubs.get(0);
    }
}
