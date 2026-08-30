package swari.sewa.module.subscription.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.subscription.dto.*;
import swari.sewa.module.subscription.entity.Subscription;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.entity.SubscriptionTransaction;
import swari.sewa.module.subscription.enums.PlanStatus;
import swari.sewa.module.subscription.enums.SubscriptionAction;
import swari.sewa.module.subscription.enums.SubscriptionStatus;
import swari.sewa.module.subscription.exception.InvalidSubscriptionStateException;
import swari.sewa.module.subscription.exception.PlanNotAvailableException;
import swari.sewa.module.subscription.exception.SubscriptionNotFoundException;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.repository.SubscriptionTransactionRepository;
import swari.sewa.module.subscription.service.SubscriptionAuditService;
import swari.sewa.module.subscription.service.SubscriptionPlanService;
import swari.sewa.module.subscription.service.SubscriptionService;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.module.employee.repository.EmployeeRepository;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTransactionRepository transactionRepository;
    private final SubscriptionPlanService planService;
    private final SubscriptionAuditService auditService;
    private final ShopOwnerRepository shopOwnerRepository;
    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;
    private final ShopRepository shopRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriberResponse> getSubscribers(String search, String status, Pageable pageable) {
        log.info("Fetching subscribers with search: '{}', status: {}", search, status);
        SubscriptionStatus statusEnum = (status != null && !status.isEmpty()) ? SubscriptionStatus.valueOf(status.toUpperCase()) : null;
        return subscriptionRepository.findWithFilters(search, statusEnum, pageable)
                .map(this::mapToSubscriberResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriberDetailsResponse getSubscriberById(Long id) {
        log.info("Fetching subscriber details by id: {}", id);

        Subscription subscription = subscriptionRepository.findByIdWithPlan(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with id: " + id));

        ShopOwner shopOwner = shopOwnerRepository.findById(subscription.getShopOwnerId()).orElse(null);
        UsageDto usage = buildUsage(subscription);
        LastPaymentInfo lastPayment = getLastPaymentInfo(subscription.getShopOwnerId());

        return SubscriberDetailsResponse.builder()
                .id(subscription.getId())
                .shopOwnerId(subscription.getShopOwnerId())
                .shopName(shopOwner != null ? shopOwner.getShopName() : null)
                .ownerName(shopOwner != null ? shopOwner.getFirstName() + " " + shopOwner.getLastName() : null)
                .planId(subscription.getPlan() != null ? subscription.getPlan().getId() : null)
                .currentPlan(subscription.getPlan() != null ? subscription.getPlan().getName() : null)
                .billingCycle(subscription.getBillingCycleSnapshot())
                .trial(subscription.getTrialId() != null)
                .trialId(subscription.getTrialId())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .autoRenewal(subscription.getAutoRenewal())
                .status(subscription.getStatus() != null ? subscription.getStatus().name() : null)
                .renewalDate(subscription.getRenewalDate())
                .lastPaymentDate(lastPayment.date())
                .lastPaymentAmount(lastPayment.amount())
                .lastTransactionUuid(lastPayment.transactionUuid())
                .lastInvoiceNumber(lastPayment.invoiceNumber())
                .usage(usage)
                .email(shopOwner != null ? shopOwner.getEmail() : null)
                .phone(shopOwner != null ? shopOwner.getPhone() : null)
                .cancelledDate(subscription.getCancelledDate())
                .suspendedDate(subscription.getSuspendedDate())
                .reason(subscription.getReason())
                .build();
    }

    @Override
    public SubscriberDetailsResponse getSubscriberByShopOwnerId(Long shopOwnerId) {
        log.info("Fetching subscriber details by shop owner id: {}", shopOwnerId);
        Subscription subscription = subscriptionRepository.findFirstByShopOwnerIdOrderByCreatedAtDesc(shopOwnerId)
                .orElseThrow(() -> new SubscriptionNotFoundException("No subscription found for shop owner id: " + shopOwnerId));
        return getSubscriberById(subscription.getId());
    }

    @Override
    public SubscriberResponse upgradeSubscription(Long id, UpgradeSubscriptionRequest request, Long adminUserId) {
        log.info("Upgrading subscription id: {} to plan {} by admin {}", id, request.getTargetPlanId(), adminUserId);

        Subscription subscription = findSubscriptionWithPlan(id);
        validateActiveStatus(subscription);

        SubscriptionPlan targetPlan = planService.getPlanEntity(request.getTargetPlanId());
        validatePlanPublished(targetPlan);

        String previousPlanName = subscription.getPlan() != null ? subscription.getPlan().getName() : null;
        subscription.setPlan(targetPlan);
        if (request.getReason() != null) {
            subscription.setReason(request.getReason());
        }
        subscription = subscriptionRepository.save(subscription);

        auditService.recordActivity(SubscriptionAction.SUBSCRIPTION_UPGRADED, "SUBSCRIPTION", subscription.getId(),
                adminUserId, "Subscription upgraded from " + previousPlanName + " to " + targetPlan.getName());

        log.info("Subscription id: {} upgraded to plan: {}", id, targetPlan.getName());
        return mapToSubscriberResponse(subscription);
    }

    @Override
    public SubscriberResponse downgradeSubscription(Long id, DowngradeSubscriptionRequest request, Long adminUserId) {
        log.info("Downgrading subscription id: {} to plan {} by admin {}", id, request.getTargetPlanId(), adminUserId);

        Subscription subscription = findSubscriptionWithPlan(id);
        validateActiveStatus(subscription);

        SubscriptionPlan targetPlan = planService.getPlanEntity(request.getTargetPlanId());
        validatePlanPublished(targetPlan);

        String previousPlanName = subscription.getPlan() != null ? subscription.getPlan().getName() : null;
        subscription.setPlan(targetPlan);
        if (request.getReason() != null) {
            subscription.setReason(request.getReason());
        }
        subscription = subscriptionRepository.save(subscription);

        auditService.recordActivity(SubscriptionAction.SUBSCRIPTION_DOWNGRADED, "SUBSCRIPTION", subscription.getId(),
                adminUserId, "Subscription downgraded from " + previousPlanName + " to " + targetPlan.getName());

        log.info("Subscription id: {} downgraded to plan: {}", id, targetPlan.getName());
        return mapToSubscriberResponse(subscription);
    }

    @Override
    public SubscriberResponse suspendSubscription(Long id, SuspendSubscriptionRequest request, Long adminUserId) {
        log.info("Suspending subscription id: {} by admin {}", id, adminUserId);

        Subscription subscription = findSubscriptionWithPlan(id);

        SubscriptionStatus currentStatus = subscription.getStatus();
        if (currentStatus != SubscriptionStatus.ACTIVE && currentStatus != SubscriptionStatus.TRIAL) {
            throw new InvalidSubscriptionStateException(
                    "Subscription must be ACTIVE or TRIAL to suspend. Current status: " + currentStatus);
        }

        subscription.setStatus(SubscriptionStatus.SUSPENDED);
        subscription.setSuspendedDate(LocalDateTime.now());
        if (request.getReason() != null) {
            subscription.setReason(request.getReason());
        }
        subscription = subscriptionRepository.save(subscription);

        auditService.recordActivity(SubscriptionAction.SUBSCRIPTION_SUSPENDED, "SUBSCRIPTION", subscription.getId(),
                adminUserId, "Subscription suspended" + (request.getReason() != null ? ": " + request.getReason() : ""));

        log.info("Subscription id: {} suspended", id);
        return mapToSubscriberResponse(subscription);
    }

    @Override
    public SubscriberResponse cancelSubscription(Long id, CancelSubscriptionRequest request, Long adminUserId) {
        log.info("Cancelling subscription id: {} by admin {}", id, adminUserId);

        Subscription subscription = findSubscriptionWithPlan(id);

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new InvalidSubscriptionStateException("Subscription is already cancelled");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledDate(LocalDateTime.now());
        if (request.getReason() != null) {
            subscription.setReason(request.getReason());
        }
        subscription = subscriptionRepository.save(subscription);

        auditService.recordActivity(SubscriptionAction.SUBSCRIPTION_CANCELLED, "SUBSCRIPTION", subscription.getId(),
                adminUserId, "Subscription cancelled" + (request.getReason() != null ? ": " + request.getReason() : ""));

        log.info("Subscription id: {} cancelled", id);
        return mapToSubscriberResponse(subscription);
    }

    private Subscription findSubscriptionWithPlan(Long id) {
        return subscriptionRepository.findByIdWithPlan(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with id: " + id));
    }

    private void validateActiveStatus(Subscription subscription) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new InvalidSubscriptionStateException(
                    "Subscription must be ACTIVE to change plan. Current status: " + subscription.getStatus());
        }
    }

    private void validatePlanPublished(SubscriptionPlan plan) {
        if (plan.getStatus() != PlanStatus.PUBLISHED) {
            throw new PlanNotAvailableException(
                    "Plan is not available for subscription. Current status: " + plan.getStatus());
        }
    }

    private SubscriberResponse mapToSubscriberResponse(Subscription subscription) {
        ShopOwner shopOwner = shopOwnerRepository.findById(subscription.getShopOwnerId()).orElse(null);
        UsageDto usage = buildUsage(subscription);
        LastPaymentInfo lastPayment = getLastPaymentInfo(subscription.getShopOwnerId());

        return SubscriberResponse.builder()
                .id(subscription.getId())
                .shopOwnerId(subscription.getShopOwnerId())
                .shopName(shopOwner != null ? shopOwner.getShopName() : null)
                .ownerName(shopOwner != null ? shopOwner.getFirstName() + " " + shopOwner.getLastName() : null)
                .planId(subscription.getPlan() != null ? subscription.getPlan().getId() : null)
                .currentPlan(subscription.getPlan() != null ? subscription.getPlan().getName() : null)
                .billingCycle(subscription.getBillingCycleSnapshot())
                .trial(subscription.getTrialId() != null)
                .trialId(subscription.getTrialId())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .autoRenewal(subscription.getAutoRenewal())
                .status(subscription.getStatus() != null ? subscription.getStatus().name() : null)
                .renewalDate(subscription.getRenewalDate())
                .lastPaymentDate(lastPayment.date())
                .lastPaymentAmount(lastPayment.amount())
                .usage(usage)
                .build();
    }

    private UsageDto buildUsage(Subscription subscription) {
        Long shopOwnerId = subscription.getShopOwnerId();
        long vehiclesUsed = vehicleRepository.countByShop_ShopOwner_Id(shopOwnerId);
        long employeesUsed = getEmployeeCountForShopOwner(subscription);

        Integer vehicleLimit = subscription.getVehicleLimitSnapshot();
        if (vehicleLimit == null && subscription.getPlan() != null) {
            vehicleLimit = subscription.getNewPlanVehicleLimit();
        }

        return UsageDto.builder()
                .vehiclesUsed(vehiclesUsed)
                .vehiclesLimit(vehicleLimit)
                .employeesUsed(employeesUsed)
                .storageUsed("N/A")
                .build();
    }

    private long getEmployeeCountForShopOwner(Subscription subscription) {
        Long shopId = subscription.getShopId();
        if (shopId == null) {
            List<Shop> shops = shopRepository.findByShopOwnerId(subscription.getShopOwnerId());
            if (shops.isEmpty()) {
                return 0L;
            }
            shopId = shops.get(0).getId();
        }
        Long count = employeeRepository.countActiveByShopId(shopId);
        return count != null ? count : 0L;
    }

    private LastPaymentInfo getLastPaymentInfo(Long shopOwnerId) {
        Page<SubscriptionTransaction> lastTxnPage = transactionRepository.findWithFilters(
                null, null, null, null, shopOwnerId, null, null, null,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "transactionDate")));

        if (lastTxnPage.hasContent()) {
            SubscriptionTransaction lastTxn = lastTxnPage.getContent().get(0);
            return new LastPaymentInfo(lastTxn.getTransactionDate(), lastTxn.getFinalAmount(), lastTxn.getTransactionId(), lastTxn.getInvoiceNumber());
        }
        return new LastPaymentInfo(null, null, null, null);
    }

    private record LastPaymentInfo(LocalDateTime date, BigDecimal amount, String transactionUuid, String invoiceNumber) {}
}
