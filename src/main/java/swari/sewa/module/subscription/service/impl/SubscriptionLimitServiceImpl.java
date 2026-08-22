package swari.sewa.module.subscription.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.subscription.entity.Subscription;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.entity.SubscriptionPlanRestriction;
import swari.sewa.module.subscription.enums.SubscriptionStatus;
import swari.sewa.module.subscription.exception.SubscriptionLimitExceededException;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.service.SubscriptionLimitService;
import swari.sewa.module.employee.repository.EmployeeRepository;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionLimitServiceImpl implements SubscriptionLimitService {

    private final SubscriptionRepository subscriptionRepository;
    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;
    private final ShopRepository shopRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean canAddVehicle(Long shopOwnerId) {
        log.debug("Checking if shop owner {} can add vehicle", shopOwnerId);

        Optional<SubscriptionPlanRestriction> restrictionOpt = getActiveRestriction(shopOwnerId);
        if (restrictionOpt.isEmpty()) {
            log.debug("No active subscription or restriction found for shop owner {}, allowing vehicle creation", shopOwnerId);
            return true;
        }

        Integer maxVehicles = restrictionOpt.get().getMaxVehicles();
        if (maxVehicles == null) {
            log.debug("No vehicle limit set for shop owner {}, allowing vehicle creation", shopOwnerId);
            return true;
        }

        long currentCount = getVehicleCount(shopOwnerId);
        boolean canAdd = currentCount < maxVehicles;
        log.debug("Vehicle limit check for shop owner {}: current={}, max={}, canAdd={}",
                shopOwnerId, currentCount, maxVehicles, canAdd);
        return canAdd;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAddEmployee(Long shopOwnerId) {
        log.debug("Checking if shop owner {} can add employee", shopOwnerId);

        Optional<SubscriptionPlanRestriction> restrictionOpt = getActiveRestriction(shopOwnerId);
        if (restrictionOpt.isEmpty()) {
            log.debug("No active subscription or restriction found for shop owner {}, allowing employee creation", shopOwnerId);
            return true;
        }

        Integer maxEmployees = restrictionOpt.get().getMaxEmployees();
        if (maxEmployees == null) {
            log.debug("No employee limit set for shop owner {}, allowing employee creation", shopOwnerId);
            return true;
        }

        long currentCount = getEmployeeCount(shopOwnerId);
        boolean canAdd = currentCount < maxEmployees;
        log.debug("Employee limit check for shop owner {}: current={}, max={}, canAdd={}",
                shopOwnerId, currentCount, maxEmployees, canAdd);
        return canAdd;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getVehicleCount(Long shopOwnerId) {
        return vehicleRepository.countByShop_ShopOwner_Id(shopOwnerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getEmployeeCount(Long shopOwnerId) {
        List<Shop> shops = shopRepository.findByShopOwnerId(shopOwnerId);
        if (shops.isEmpty()) {
            return 0L;
        }
        Long shopId = shops.get(0).getId();
        Long count = employeeRepository.countActiveByShopId(shopId);
        return count != null ? count : 0L;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateVehicleLimit(Long shopOwnerId) {
        if (!canAddVehicle(shopOwnerId)) {
            Optional<SubscriptionPlanRestriction> restrictionOpt = getActiveRestriction(shopOwnerId);
            Integer maxVehicles = restrictionOpt.map(SubscriptionPlanRestriction::getMaxVehicles).orElse(null);
            long currentCount = getVehicleCount(shopOwnerId);
            throw new SubscriptionLimitExceededException(
                    "Vehicle limit exceeded. Current: " + currentCount + ", Maximum allowed: " + maxVehicles);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateEmployeeLimit(Long shopOwnerId) {
        if (!canAddEmployee(shopOwnerId)) {
            Optional<SubscriptionPlanRestriction> restrictionOpt = getActiveRestriction(shopOwnerId);
            Integer maxEmployees = restrictionOpt.map(SubscriptionPlanRestriction::getMaxEmployees).orElse(null);
            long currentCount = getEmployeeCount(shopOwnerId);
            throw new SubscriptionLimitExceededException(
                    "Employee limit exceeded. Current: " + currentCount + ", Maximum allowed: " + maxEmployees);
        }
    }

    private Optional<SubscriptionPlanRestriction> getActiveRestriction(Long shopOwnerId) {
        Optional<Subscription> subscriptionOpt = findActiveSubscription(shopOwnerId);
        if (subscriptionOpt.isEmpty()) {
            return Optional.empty();
        }

        SubscriptionPlan plan = subscriptionOpt.get().getPlan();
        if (plan == null || plan.getRestrictions() == null || plan.getRestrictions().isEmpty()) {
            return Optional.empty();
        }

        return plan.getRestrictions().stream().findFirst();
    }

    private Optional<Subscription> findActiveSubscription(Long shopOwnerId) {
        List<Subscription> activeSubs = subscriptionRepository.findByShopOwnerIdAndStatus(shopOwnerId, SubscriptionStatus.ACTIVE);
        if (!activeSubs.isEmpty()) {
            return Optional.of(activeSubs.get(0));
        }

        List<Subscription> trialSubs = subscriptionRepository.findByShopOwnerIdAndStatus(shopOwnerId, SubscriptionStatus.TRIAL);
        if (!trialSubs.isEmpty()) {
            return Optional.of(trialSubs.get(0));
        }

        return Optional.empty();
    }
}
