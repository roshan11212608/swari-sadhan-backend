package swari.sewa.module.payment.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.payment.config.FonepayConfig;
import swari.sewa.module.payment.dto.CreatePaymentRequest;
import swari.sewa.module.payment.dto.PaymentResponse;
import swari.sewa.module.payment.entity.Payment;
import swari.sewa.module.payment.enums.PaymentStatus;
import swari.sewa.module.payment.exception.PaymentException;
import swari.sewa.module.payment.repository.PaymentRepository;
import swari.sewa.module.payment.service.FonepayPaymentService;
import swari.sewa.module.payment.service.PaymentEmailService;
import swari.sewa.module.payment.service.PaymentExpenseSyncService;
import swari.sewa.module.payment.service.FonepaySignatureService;
import swari.sewa.module.subscription.entity.Subscription;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.entity.SubscriptionTransaction;
import swari.sewa.module.subscription.enums.PlanStatus;
import swari.sewa.module.subscription.enums.SubscriptionStatus;
import swari.sewa.module.subscription.enums.TransactionStatus;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.repository.SubscriptionTransactionRepository;
import swari.sewa.module.subscription.service.InvoiceService;
import swari.sewa.module.subscription.service.SubscriptionPlanService;
import swari.sewa.module.subscription.service.SubscriptionSettingsService;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FonepayPaymentServiceImpl implements FonepayPaymentService {

    private final PaymentRepository paymentRepository;
    private final FonepayConfig fonepayConfig;
    private final FonepaySignatureService signatureService;
    private final SubscriptionPlanService planService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTransactionRepository transactionRepository;
    private final InvoiceService invoiceService;
    private final PaymentEmailService paymentEmailService;
    private final PaymentExpenseSyncService paymentExpenseSyncService;
    private final SubscriptionSettingsService settingsService;
    private final VehicleRepository vehicleRepository;

    private static final DateTimeFormatter PRN_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public String createPayment(CreatePaymentRequest request, Long shopOwnerId) {
        log.info("Creating Fonepay payment for shop_owner={}, plan={}, cycle={}",
                shopOwnerId, request.getPlanId(), request.getBillingCycle());

        // 1. Fetch plan from DB — never trust frontend price
        SubscriptionPlan plan = planService.getPlanEntity(request.getPlanId());
        if (plan.getStatus() != PlanStatus.PUBLISHED) {
            throw new PaymentException("Selected plan is not currently available");
        }

        // 2. Get price from DB
        BigDecimal amount = getPriceForCycle(plan, request.getBillingCycle());
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Invalid plan price for the selected billing cycle");
        }

        // 3. Tax — based on subscription settings (VAT enable + percentage)
        BigDecimal taxAmount;
        var settings = settingsService.getSettingsEntity();
        if (Boolean.TRUE.equals(settings.getEnableVat()) && settings.getTaxPercentage() != null && settings.getTaxPercentage() > 0) {
            taxAmount = amount.multiply(BigDecimal.valueOf(settings.getTaxPercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal totalAmount = amount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        // 3. Generate unique PRN (Payment Reference Number)
        String prn = generatePrn();
        while (paymentRepository.existsByTransactionUuid(prn)) {
            prn = generatePrn();
        }

        // 4. Create PENDING payment record
        Payment payment = Payment.builder()
                .transactionUuid(prn)
                .gateway("FONEPAY")
                .shopOwnerId(shopOwnerId)
                .subscriptionPlanId(plan.getId())
                .billingCycle(request.getBillingCycle())
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .currency("NPR")
                .status(PaymentStatus.PENDING)
                .productCode(fonepayConfig.getMerchantCodePid())
                .build();
        payment = paymentRepository.save(payment);
        log.info("Fonepay payment record created: id={}, prn={}", payment.getId(), prn);

        // 5. Build signed Fonepay redirect URL
        // Signed string format: PID,MD,PRN,AMT,CRN,DT,R1,R2,RU
        String md = "P"; // Payment mode
        String crn = "NPR"; // Currency
        String dt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        String r1 = "Subscription " + plan.getName() + " " + request.getBillingCycle();
        String r2 = "Shop owner ID: " + shopOwnerId;
        String ru = fonepayConfig.getBackendReturnUrl();

        String signedString = fonepayConfig.getMerchantCodePid() + "," +
                md + "," + prn + "," + totalAmount.toPlainString() + "," +
                crn + "," + dt + "," + r1 + "," + r2 + "," + ru;

        String dv = signatureService.generateSignature(signedString, fonepayConfig.getMerchantSecretKey());

        // 6. Build redirect URL
        StringBuilder url = new StringBuilder(fonepayConfig.getPaymentUrl());
        url.append("/api/merchantRequest?");
        url.append("PID=").append(fonepayConfig.getMerchantCodePid());
        url.append("&MD=").append(md);
        url.append("&AMT=").append(totalAmount.toPlainString());
        url.append("&CRN=").append(crn);
        url.append("&R1=").append(URLEncoder.encode(r1, StandardCharsets.UTF_8));
        url.append("&R2=").append(URLEncoder.encode(r2, StandardCharsets.UTF_8));
        url.append("&DT=").append(URLEncoder.encode(dt, StandardCharsets.UTF_8));
        url.append("&PRN=").append(URLEncoder.encode(prn, StandardCharsets.UTF_8));
        url.append("&RU=").append(URLEncoder.encode(ru, StandardCharsets.UTF_8));
        url.append("&DV=").append(dv);

        log.info("Fonepay redirect URL generated for prn={}", prn);
        return url.toString();
    }

    @Override
    public PaymentResponse handleCallback(String prn, String pid, String ps, String amt,
                                          String uid, String bid, String dv) {
        log.info("Fonepay callback received: prn={}, ps={}", prn, ps);

        Payment payment = paymentRepository.findByTransactionUuid(prn)
                .orElseThrow(() -> new PaymentException("Payment not found for prn: " + prn));

        // Idempotency
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment {} already SUCCESS — skipping duplicate", prn);
            return mapToResponse(payment);
        }

        // Verify signature
        // Verification string: PID,AMT,PRN,BID,UID
        String verifyString = pid + "," + amt + "," + prn + "," + bid + "," + uid;
        if (!signatureService.verifySignature(verifyString, fonepayConfig.getMerchantSecretKey(), dv)) {
            log.error("Fonepay signature verification failed for prn={}", prn);
            payment.setStatus(PaymentStatus.VERIFICATION_FAILED);
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);
            return mapToResponse(payment);
        }

        // Verify product code
        if (!fonepayConfig.getMerchantCodePid().equals(pid)) {
            log.error("Fonepay PID mismatch: expected={}, got={}", fonepayConfig.getMerchantCodePid(), pid);
            payment.setStatus(PaymentStatus.VERIFICATION_FAILED);
            payment.setFailureReason("Merchant code mismatch");
            paymentRepository.save(payment);
            return mapToResponse(payment);
        }

        // Verify amount
        BigDecimal expectedTotal = payment.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal verifiedTotal = new BigDecimal(amt).setScale(2, RoundingMode.HALF_UP);
        if (verifiedTotal.compareTo(expectedTotal) != 0) {
            log.error("Amount mismatch: expected={}, verified={}", expectedTotal, verifiedTotal);
            payment.setStatus(PaymentStatus.VERIFICATION_FAILED);
            payment.setFailureReason("Amount mismatch");
            paymentRepository.save(payment);
            return mapToResponse(payment);
        }

        // Check status — only "success" activates
        if (!"success".equalsIgnoreCase(ps)) {
            log.warn("Fonepay status not success: {} for prn={}", ps, prn);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Fonepay status: " + ps);
            paymentRepository.save(payment);
            return mapToResponse(payment);
        }

        // === SUCCESS ===
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setGatewayTransactionId(uid);
        payment.setGatewayRefId(bid);
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        log.info("Fonepay payment {} verified and marked SUCCESS", prn);

        // Generate invoice — idempotent
        if (payment.getInvoiceNumber() == null || payment.getInvoiceNumber().isEmpty()) {
            String invoiceNumber = invoiceService.generateInvoiceNumber();
            payment.setInvoiceNumber(invoiceNumber);
            payment = paymentRepository.save(payment);
            log.info("Invoice {} generated for payment {}", invoiceNumber, prn);
        }

        // Activate subscription — idempotent
        if (payment.getSubscriptionId() == null) {
            activateSubscription(payment);
            log.info("Subscription activated for payment {}", prn);
        }

        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByPrn(String prn) {
        Payment payment = paymentRepository.findByTransactionUuid(prn)
                .orElseThrow(() -> new PaymentException("Payment not found for prn: " + prn));
        return mapToResponse(payment);
    }

    // ===== Helpers =====

    private void activateSubscription(Payment payment) {
        SubscriptionPlan plan = planService.getPlanEntity(payment.getSubscriptionPlanId());

        // Expire any existing TRIAL subscription so the unique constraint
        // uk_subscriptions_active_owner (one ACTIVE-or-TRIAL per owner) is
        // not violated when the new ACTIVE subscription is inserted.
        subscriptionRepository.findTrialByShopOwnerId(payment.getShopOwnerId())
                .ifPresent(trial -> {
                    trial.setStatus(SubscriptionStatus.EXPIRED);
                    subscriptionRepository.saveAndFlush(trial);
                    log.info("Expired trial subscription {} for shop_owner={} upon paid activation",
                            trial.getId(), payment.getShopOwnerId());
                });

        // Check if there's already an ACTIVE subscription (e.g. user is renewing
        // or upgrading). If so, renew/extend it instead of creating a new one.
        List<Subscription> existingActive = subscriptionRepository
                .findByShopOwnerIdAndStatus(payment.getShopOwnerId(), SubscriptionStatus.ACTIVE);

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = calculateEndDate(startDate, payment.getBillingCycle());

        // Snapshot plan details so later admin changes don't affect this subscription
        // Same logic as EsewaPaymentServiceImpl: monthly vehicle limit × billing cycle months
        Integer monthlyVehicleLimit = null;
        if (plan.getRestrictions() != null) {
            for (var r : plan.getRestrictions()) {
                if (r.getMaxVehicles() != null) {
                    monthlyVehicleLimit = r.getMaxVehicles();
                    break;
                }
            }
        }
        Integer vehicleLimit = monthlyVehicleLimit != null
                ? monthlyVehicleLimit * getMonthsInCycle(payment.getBillingCycle())
                : null;

        Subscription subscription;
        if (!existingActive.isEmpty()) {
            // Renew/extend the existing ACTIVE subscription
            subscription = existingActive.get(0);

            // === Vehicle allowance rollover calculation ===
            // Count vehicles created after the old currentPeriodStart (all vehicles
            // from the old period). At this point, no new-period vehicles exist yet.
            int carriedForward = 0;
            if (subscription.getVehicleLimitSnapshot() != null && subscription.getCurrentPeriodStart() != null) {
                // Truncate to seconds to match MySQL DATETIME precision
                LocalDateTime oldPeriodStart = subscription.getCurrentPeriodStart()
                        .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
                long oldUsed = vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(
                        payment.getShopOwnerId(), oldPeriodStart);
                int oldLimit = subscription.getVehicleLimitSnapshot();
                carriedForward = Math.max(0, oldLimit - (int) oldUsed);
                log.info("Rollover: oldPeriodStart={}, oldLimit={}, oldUsed={}, carriedForward={}",
                        oldPeriodStart, oldLimit, oldUsed, carriedForward);
            }

            // If the current subscription hasn't expired yet, extend from its end date;
            // otherwise, start from now.
            LocalDateTime baseDate = subscription.getEndDate() != null
                    && subscription.getEndDate().isAfter(startDate)
                    ? subscription.getEndDate()
                    : startDate;
            endDate = calculateEndDate(baseDate, payment.getBillingCycle());

            // Total vehicle limit = new plan limit + carried-forward unused allowance
            int totalVehicleLimit = vehicleLimit != null ? vehicleLimit + carriedForward : null;

            // startDate stays as the ORIGINAL subscription start (preserves history).
            // currentPeriodStart is set to NOW (the renewal purchase date) so the new
            // vehicle allowance takes effect immediately.
            subscription.setPlan(plan);
            subscription.setEndDate(endDate);
            subscription.setRenewalDate(endDate);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setCurrentPeriodStart(startDate);
            subscription.setPlanNameSnapshot(plan.getName());
            subscription.setPlanDescriptionSnapshot(plan.getShortDescription() != null ? plan.getShortDescription() : plan.getDescription());
            subscription.setPlanIconSnapshot(plan.getIcon());
            subscription.setPlanThemeColorSnapshot(plan.getThemeColor());
            subscription.setVehicleLimitSnapshot(totalVehicleLimit);
            subscription.setNewPlanVehicleLimit(vehicleLimit);
            subscription.setCarriedForwardVehicleLimit(carriedForward);
            subscription.setPricePaid(payment.getAmount());
            subscription.setBillingCycleSnapshot(payment.getBillingCycle());
            subscription = subscriptionRepository.saveAndFlush(subscription);
            log.info("Subscription {} renewed/extended for shop_owner={} until {} (period starts {}, totalLimit={} = {}+{})",
                    subscription.getId(), payment.getShopOwnerId(), endDate, baseDate, totalVehicleLimit, vehicleLimit, carriedForward);
        } else {
            subscription = Subscription.builder()
                    .shopOwnerId(payment.getShopOwnerId())
                    .plan(plan)
                    .startDate(startDate)
                    .currentPeriodStart(startDate)
                    .endDate(endDate)
                    .renewalDate(endDate)
                    .status(SubscriptionStatus.ACTIVE)
                    .autoRenewal(false)
                    .planNameSnapshot(plan.getName())
                    .planDescriptionSnapshot(plan.getShortDescription() != null ? plan.getShortDescription() : plan.getDescription())
                    .planIconSnapshot(plan.getIcon())
                    .planThemeColorSnapshot(plan.getThemeColor())
                    .vehicleLimitSnapshot(vehicleLimit)
                    .newPlanVehicleLimit(vehicleLimit)
                    .carriedForwardVehicleLimit(0)
                    .pricePaid(payment.getAmount())
                    .billingCycleSnapshot(payment.getBillingCycle())
                    .build();
            subscription = subscriptionRepository.save(subscription);
            log.info("Subscription {} activated for shop_owner={}", subscription.getId(), payment.getShopOwnerId());
        }

        payment.setSubscriptionId(subscription.getId());
        // Freeze plan name and subscription period on the payment so that
        // later renewals/upgrades (which overwrite the Subscription row's
        // snapshots) do not destroy the historical record of what plan the
        // user had at this billing period.
        payment.setPlanNameSnapshot(plan.getName());
        payment.setSubscriptionStartDateSnapshot(subscription.getCurrentPeriodStart());
        payment.setSubscriptionEndDateSnapshot(subscription.getEndDate());
        payment.setVehicleLimitSnapshot(subscription.getVehicleLimitSnapshot());
        paymentRepository.save(payment);

        // Create SubscriptionTransaction record for Super Admin transactions page
        createTransactionRecord(payment, plan, subscription.getId());

        // Send payment success email with invoice details
        paymentEmailService.sendPaymentSuccessEmail(payment, plan);

        // Auto-create an expense record so it shows in the expenses list
        paymentExpenseSyncService.createSubscriptionExpense(payment, plan);
    }

    private SubscriptionTransaction createTransactionRecord(Payment payment, SubscriptionPlan plan, Long subscriptionId) {
        // Idempotency: transactionId is uniquely constrained (uk_sub_txn_id). A
        // replayed gateway callback must reuse the existing record rather than
        // attempting a second insert.
        var existing = transactionRepository.findByTransactionId(payment.getTransactionUuid());
        if (existing.isPresent()) {
            log.info("SubscriptionTransaction already exists for payment {} — reusing id={}",
                    payment.getTransactionUuid(), existing.get().getId());
            return existing.get();
        }

        SubscriptionTransaction transaction = SubscriptionTransaction.builder()
                .transactionId(payment.getTransactionUuid())
                .subscriptionId(subscriptionId)
                .shopOwnerId(payment.getShopOwnerId())
                .plan(plan)
                .amount(payment.getAmount())
                .tax(payment.getTaxAmount())
                .couponId(payment.getCouponId())
                .couponCodeSnapshot(payment.getCouponCodeSnapshot())
                .discount(payment.getDiscountAmount() != null ? payment.getDiscountAmount() : BigDecimal.ZERO)
                .finalAmount(payment.getTotalAmount())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "FONEPAY")
                .gateway(payment.getGateway())
                .status(TransactionStatus.COMPLETED)
                .invoiceNumber(payment.getInvoiceNumber())
                .transactionDate(payment.getPaidAt() != null ? payment.getPaidAt() : LocalDateTime.now())
                .build();
        transaction = transactionRepository.save(transaction);
        log.info("SubscriptionTransaction created for payment {}", payment.getTransactionUuid());
        return transaction;
    }

    private LocalDateTime calculateEndDate(LocalDateTime startDate, String billingCycle) {
        if (billingCycle == null) return startDate.plusMonths(1);
        switch (billingCycle.toLowerCase()) {
            case "monthly": return startDate.plusMonths(1);
            case "quarterly": return startDate.plusMonths(3);
            case "halfyearly":
            case "half_yearly": return startDate.plusMonths(6);
            case "yearly": return startDate.plusYears(1);
            default: return startDate.plusMonths(1);
        }
    }

    /**
     * Returns the number of months in a billing cycle.
     * Used to calculate the total vehicle limit: monthlyLimit × monthsInCycle.
     */
    private int getMonthsInCycle(String billingCycle) {
        if (billingCycle == null) return 1;
        switch (billingCycle.toLowerCase()) {
            case "monthly": return 1;
            case "quarterly": return 3;
            case "halfyearly":
            case "half_yearly": return 6;
            case "yearly": return 12;
            default: return 1;
        }
    }

    private BigDecimal getPriceForCycle(SubscriptionPlan plan, String billingCycle) {
        Set<swari.sewa.module.subscription.entity.SubscriptionPlanPricing> pricings = plan.getPricings();
        if (pricings == null || pricings.isEmpty()) {
            throw new PaymentException("No pricing configured for this plan");
        }
        swari.sewa.module.subscription.entity.SubscriptionPlanPricing pricing = pricings.iterator().next();
        switch (billingCycle.toLowerCase()) {
            case "monthly": return pricing.getMonthly();
            case "quarterly": return pricing.getQuarterly();
            case "halfyearly":
            case "half_yearly": return pricing.getHalfYearly();
            case "yearly": return pricing.getYearly();
            default: throw new PaymentException("Invalid billing cycle: " + billingCycle);
        }
    }

    private String generatePrn() {
        String timestamp = LocalDateTime.now().format(PRN_FMT);
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format("FP%s%04d", timestamp, random);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionUuid(payment.getTransactionUuid())
                .gateway(payment.getGateway())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .gatewayRefId(payment.getGatewayRefId())
                .shopOwnerId(payment.getShopOwnerId())
                .subscriptionPlanId(payment.getSubscriptionPlanId())
                .subscriptionId(payment.getSubscriptionId())
                .billingCycle(payment.getBillingCycle())
                .amount(payment.getAmount())
                .taxAmount(payment.getTaxAmount())
                .totalAmount(payment.getTotalAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .paymentMethod(payment.getPaymentMethod())
                .invoiceNumber(payment.getInvoiceNumber())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
