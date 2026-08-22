package swari.sewa.module.payment.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import swari.sewa.module.payment.config.EsewaConfig;
import swari.sewa.module.payment.dto.CreatePaymentRequest;
import swari.sewa.module.payment.dto.CreatePaymentResponse;
import swari.sewa.module.payment.dto.PaymentResponse;
import swari.sewa.module.payment.entity.Payment;
import swari.sewa.module.payment.enums.PaymentStatus;
import swari.sewa.module.payment.repository.PaymentRepository;
import swari.sewa.module.payment.service.EsewaPaymentService;
import swari.sewa.module.payment.service.EsewaSignatureService;
import swari.sewa.module.payment.service.PaymentEmailService;
import swari.sewa.module.payment.service.PaymentExpenseSyncService;
import swari.sewa.module.payment.service.CouponUsageRecorder;
import swari.sewa.module.subscription.entity.Subscription;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.entity.SubscriptionPlanPricing;
import swari.sewa.module.subscription.entity.SubscriptionPlanRestriction;
import swari.sewa.module.subscription.entity.SubscriptionTransaction;
import swari.sewa.module.subscription.enums.PlanStatus;
import swari.sewa.module.subscription.enums.SubscriptionStatus;
import swari.sewa.module.subscription.enums.TransactionStatus;
import swari.sewa.module.payment.exception.PaymentException;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.repository.SubscriptionTransactionRepository;
import swari.sewa.module.subscription.repository.SubscriptionCouponUsageRepository;
import swari.sewa.module.subscription.service.InvoiceService;
import swari.sewa.module.subscription.service.SubscriptionCouponService;
import swari.sewa.module.subscription.service.SubscriptionPlanService;
import swari.sewa.module.subscription.service.SubscriptionSettingsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EsewaPaymentServiceImpl implements EsewaPaymentService {

    private final PaymentRepository paymentRepository;
    private final EsewaConfig esewaConfig;
    private final EsewaSignatureService signatureService;
    private final SubscriptionPlanService planService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTransactionRepository transactionRepository;
    private final InvoiceService invoiceService;
    private final PaymentEmailService paymentEmailService;
    private final PaymentExpenseSyncService paymentExpenseSyncService;
    private final SubscriptionSettingsService settingsService;
    private final SubscriptionCouponService couponService;
    private final SubscriptionCouponUsageRepository couponUsageRepository;
    private final CouponUsageRecorder couponUsageRecorder;

    private static final String SIGNED_FIELD_NAMES = "total_amount,transaction_uuid,product_code";
    private static final DateTimeFormatter TXN_UUID_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest request, Long shopOwnerId) {
        log.info("Creating eSewa payment for shop_owner={}, plan={}, cycle={}",
                shopOwnerId, request.getPlanId(), request.getBillingCycle());

        // 1. Fetch the plan from DB — never trust frontend price
        SubscriptionPlan plan = planService.getPlanEntity(request.getPlanId());

        // 2. Validate plan is published
        if (plan.getStatus() != PlanStatus.PUBLISHED) {
            throw new PaymentException("Selected plan is not currently available");
        }

        // 3. Get the price from DB based on billing cycle
        BigDecimal amount = getPriceForCycle(plan, request.getBillingCycle());
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Invalid plan price for the selected billing cycle");
        }

        // 3b. Apply coupon if provided — revalidates with pessimistic lock to prevent race conditions
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        Long couponId = null;
        String couponCodeSnapshot = null;
        String couponDiscountTypeSnapshot = null;
        String couponDiscountValueSnapshot = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            // Use locked validation to prevent concurrent requests from exceeding usage limit
            var couponResult = couponService.validateCouponForPayment(request.getCouponCode(), amount);
            if (!Boolean.TRUE.equals(couponResult.getValid())) {
                throw new PaymentException(couponResult.getMessage());
            }
            discountAmount = couponResult.getDiscountAmount();
            couponId = couponResult.getCouponId();
            // Snapshot coupon details for historical preservation
            couponCodeSnapshot = couponResult.getCode();
            couponDiscountTypeSnapshot = couponResult.getDiscountType();
            if ("PERCENTAGE".equals(couponDiscountTypeSnapshot) && couponResult.getPercentage() != null) {
                couponDiscountValueSnapshot = couponResult.getPercentage() + "%";
            } else if ("FLAT".equals(couponDiscountTypeSnapshot) && couponResult.getFlatDiscount() != null) {
                couponDiscountValueSnapshot = couponResult.getFlatDiscount().toPlainString();
            }
            log.info("Coupon {} applied with lock: discount={}, final amount before tax={}",
                    couponResult.getCode(), discountAmount, amount.subtract(discountAmount));
        }
        BigDecimal amountAfterDiscount = amount.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        // 4. Tax — based on subscription settings (VAT enable + percentage)
        BigDecimal taxAmount;
        var settings = settingsService.getSettingsEntity();
        if (Boolean.TRUE.equals(settings.getEnableVat()) && settings.getTaxPercentage() != null && settings.getTaxPercentage() > 0) {
            taxAmount = amountAfterDiscount.multiply(BigDecimal.valueOf(settings.getTaxPercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal totalAmount = amountAfterDiscount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        // 5. Generate unique transaction UUID
        String transactionUuid = generateTransactionUuid();
        while (paymentRepository.existsByTransactionUuid(transactionUuid)) {
            transactionUuid = generateTransactionUuid();
        }

        // 6. Create PENDING payment record
        Payment payment = Payment.builder()
                .transactionUuid(transactionUuid)
                .gateway("ESEWA")
                .shopOwnerId(shopOwnerId)
                .subscriptionPlanId(plan.getId())
                .billingCycle(request.getBillingCycle())
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .discountAmount(discountAmount)
                .couponId(couponId)
                .couponCodeSnapshot(couponCodeSnapshot)
                .couponDiscountTypeSnapshot(couponDiscountTypeSnapshot)
                .couponDiscountValueSnapshot(couponDiscountValueSnapshot)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .currency("NPR")
                .status(PaymentStatus.PENDING)
                .productCode(esewaConfig.getProductCode())
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment record created: id={}, transaction_uuid={}", payment.getId(), transactionUuid);

        // 7. Generate signature — eSewa expects amounts as integers (e.g. "499" not "499.00")
        // eSewa validates: total_amount = amount + tax_amount + service_charge + delivery_charge
        // So "amount" sent to eSewa must be the DISCOUNTED base amount (after coupon), not the original plan price.
        String totalAmountForEsewa = toEsewaAmount(totalAmount);
        String amountForEsewa = toEsewaAmount(amountAfterDiscount);
        String taxAmountForEsewa = toEsewaAmount(taxAmount);

        String signature = signatureService.generateSignature(
                totalAmountForEsewa,
                transactionUuid,
                esewaConfig.getProductCode(),
                esewaConfig.getSecretKey()
        );

        // 8. Build response with all eSewa form parameters
        return CreatePaymentResponse.builder()
                .paymentUrl(esewaConfig.getPaymentUrl())
                .transactionUuid(transactionUuid)
                .productCode(esewaConfig.getProductCode())
                .amount(amountForEsewa)
                .taxAmount(taxAmountForEsewa)
                .totalAmount(totalAmountForEsewa)
                .signedFieldNames(SIGNED_FIELD_NAMES)
                .signature(signature)
                .successUrl(esewaConfig.getBackendSuccessUrl())
                .failureUrl(esewaConfig.getBackendFailureUrl())
                .productServiceCharge("0")
                .productDeliveryCharge("0")
                .currency("NPR")
                .paymentId(payment.getId())
                .amountValue(amount)
                .totalAmountValue(totalAmount)
                .build();
    }

    @Override
    public PaymentResponse handleSuccessCallback(String transactionUuid, String totalAmount,
                                                  String transactionCode, String refId, String status) {
        log.info("eSewa success callback received: transaction_uuid={}", transactionUuid);

        Payment payment = paymentRepository.findByTransactionUuid(transactionUuid)
                .orElseThrow(() -> new PaymentException("Payment not found for transaction_uuid: " + transactionUuid));

        // Idempotency: if already SUCCESS, return without reprocessing
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment {} already processed as SUCCESS — skipping duplicate", transactionUuid);
            return mapToResponse(payment);
        }

        // If payment was already failed/cancelled, don't process success
        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.CANCELLED) {
            log.warn("Payment {} was already marked as {} — ignoring success callback",
                    transactionUuid, payment.getStatus());
            return mapToResponse(payment);
        }

        // Verify the transaction with eSewa status API
        try {
            EsewaStatusResult statusResult = verifyWithEsewa(payment);

            // Validate transaction UUID (from callback vs DB)
            if (!transactionUuid.equals(payment.getTransactionUuid())) {
                log.error("Transaction UUID mismatch: expected={}, got={}", payment.getTransactionUuid(), transactionUuid);
                payment.setStatus(PaymentStatus.VERIFICATION_FAILED);
                payment.setFailureReason("Transaction UUID mismatch");
                paymentRepository.save(payment);
                return mapToResponse(payment);
            }

            // Validate product code (from eSewa status response vs config)
            if (!esewaConfig.getProductCode().equals(statusResult.productCode)) {
                log.error("Product code mismatch: expected={}, got={}", esewaConfig.getProductCode(), statusResult.productCode);
                payment.setStatus(PaymentStatus.VERIFICATION_FAILED);
                payment.setFailureReason("Product code mismatch");
                paymentRepository.save(payment);
                return mapToResponse(payment);
            }

            // Validate amount (from eSewa status response vs DB)
            if (statusResult.totalAmount == null) {
                log.error("eSewa status response missing totalAmount");
                payment.setStatus(PaymentStatus.VERIFICATION_FAILED);
                payment.setFailureReason("eSewa response missing totalAmount");
                paymentRepository.save(payment);
                return mapToResponse(payment);
            }
            // eSewa returns amount as integer (e.g. "499"), compare against DB total as integer
            BigDecimal expectedTotal = payment.getTotalAmount().setScale(0, RoundingMode.HALF_UP);
            BigDecimal verifiedTotal = new BigDecimal(statusResult.totalAmount).setScale(0, RoundingMode.HALF_UP);
            if (verifiedTotal.compareTo(expectedTotal) != 0) {
                log.error("Amount mismatch: expected={}, verified={}", expectedTotal, verifiedTotal);
                payment.setStatus(PaymentStatus.VERIFICATION_FAILED);
                payment.setFailureReason("Amount mismatch: expected " + expectedTotal + ", got " + verifiedTotal);
                paymentRepository.save(payment);
                return mapToResponse(payment);
            }

            // Validate status — only COMPLETE activates subscription
            String esewaStatus = statusResult.status != null ? statusResult.status.toUpperCase() : null;
            if (esewaStatus == null) {
                log.error("eSewa status response missing status field");
                payment.setStatus(PaymentStatus.VERIFICATION_FAILED);
                payment.setFailureReason("eSewa response missing status");
                paymentRepository.save(payment);
                return mapToResponse(payment);
            }

            switch (esewaStatus) {
                case "COMPLETE":
                    // All validations passed — mark SUCCESS
                    break;
                case "PENDING":
                    log.warn("eSewa status PENDING for {} — payment not yet complete", transactionUuid);
                    // Leave as PENDING — can be retried later
                    return mapToResponse(payment);
                case "CANCELED":
                case "NOT_FOUND":
                    log.warn("eSewa status {} for {} — marking as CANCELLED", esewaStatus, transactionUuid);
                    payment.setStatus(PaymentStatus.CANCELLED);
                    payment.setFailureReason("eSewa status: " + esewaStatus);
                    paymentRepository.save(payment);
                    return mapToResponse(payment);
                case "FULL_REFUND":
                case "PARTIAL_REFUND":
                    log.warn("eSewa status {} for {} — marking as FAILED", esewaStatus, transactionUuid);
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason("Payment refunded: " + esewaStatus);
                    paymentRepository.save(payment);
                    return mapToResponse(payment);
                case "AMBIGUOUS":
                case "AMBIGIOUS": // eSewa docs have a typo
                    log.warn("eSewa status AMBIGUOUS for {} — marking as VERIFICATION_FAILED", transactionUuid);
                    payment.setStatus(PaymentStatus.VERIFICATION_FAILED);
                    payment.setFailureReason("eSewa status: AMBIGUOUS");
                    paymentRepository.save(payment);
                    return mapToResponse(payment);
                default:
                    log.error("Unknown eSewa status: {} for {}", esewaStatus, transactionUuid);
                    payment.setStatus(PaymentStatus.VERIFICATION_FAILED);
                    payment.setFailureReason("Unknown eSewa status: " + esewaStatus);
                    paymentRepository.save(payment);
                    return mapToResponse(payment);
            }

            // === COMPLETE: mark SUCCESS, generate invoice, activate subscription ===
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayTransactionId(transactionCode);
            payment.setGatewayRefId(refId != null ? refId : statusResult.refId);
            payment.setPaidAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);
            log.info("Payment {} verified and marked SUCCESS", transactionUuid);

            // Generate invoice — idempotent: only if not already generated
            if (payment.getInvoiceNumber() == null || payment.getInvoiceNumber().isEmpty()) {
                String invoiceNumber = invoiceService.generateInvoiceNumber();
                payment.setInvoiceNumber(invoiceNumber);
                payment = paymentRepository.save(payment);
                log.info("Invoice {} generated for payment {}", invoiceNumber, transactionUuid);
            } else {
                log.info("Invoice {} already exists for payment {} — skipping", payment.getInvoiceNumber(), transactionUuid);
            }

            // Activate subscription — idempotent: only if not already activated
            if (payment.getSubscriptionId() == null) {
                activateSubscription(payment);
                log.info("Subscription activated for payment {}", transactionUuid);
            } else {
                log.info("Subscription {} already exists for payment {} — skipping", payment.getSubscriptionId(), transactionUuid);
            }

            // Record coupon usage — LAST step, in a separate transaction with primitives only
            // This must be after all critical operations so it can never corrupt the main transaction
            if (payment.getCouponId() != null) {
                try {
                    couponUsageRecorder.recordUsage(
                            payment.getCouponId(),
                            payment.getId(),
                            payment.getShopOwnerId(),
                            payment.getDiscountAmount());
                } catch (Exception e) {
                    log.error("Coupon usage recording failed for payment {} (non-fatal): {}", payment.getId(), e.getMessage());
                }
            }

        } catch (PaymentException e) {
            // Don't overwrite a SUCCESS that may have been set before the exception
            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.VERIFICATION_FAILED);
                payment.setFailureReason(e.getMessage());
                paymentRepository.save(payment);
            }
            log.error("Payment verification failed for {}: {}", transactionUuid, e.getMessage());
        }

        return mapToResponse(payment);
    }

    @Override
    public PaymentResponse handleFailureCallback(String transactionUuid, String failureReason) {
        log.info("eSewa failure callback received: transaction_uuid={}, reason={}", transactionUuid, failureReason);

        Payment payment = paymentRepository.findByTransactionUuid(transactionUuid)
                .orElseThrow(() -> new PaymentException("Payment not found for transaction_uuid: " + transactionUuid));

        // Idempotency: don't change if already SUCCESS
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment {} already SUCCESS — ignoring failure callback", transactionUuid);
            return mapToResponse(payment);
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason != null ? failureReason : "Payment failed at eSewa");
        payment = paymentRepository.save(payment);
        log.info("Payment {} marked as FAILED", transactionUuid);

        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionUuid(String transactionUuid) {
        Payment payment = paymentRepository.findByTransactionUuid(transactionUuid)
                .orElseThrow(() -> new PaymentException("Payment not found for transaction_uuid: " + transactionUuid));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public CreatePaymentResponse getEsewaFormParams(String transactionUuid) {
        Payment payment = paymentRepository.findByTransactionUuid(transactionUuid)
                .orElseThrow(() -> new PaymentException("Payment not found for transaction_uuid: " + transactionUuid));

        // Only allow regenerating form params for PENDING payments
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException("Payment is no longer pending (status: " + payment.getStatus() + ")");
        }

        // Regenerate signature with the stored amounts
        // eSewa validates: total_amount = amount + tax_amount, so amount must be the discounted base
        String totalAmountForEsewa = toEsewaAmount(payment.getTotalAmount());
        BigDecimal discountedBase = payment.getAmount().subtract(
                payment.getDiscountAmount() != null ? payment.getDiscountAmount() : BigDecimal.ZERO);
        String amountForEsewa = toEsewaAmount(discountedBase);
        String taxAmountForEsewa = toEsewaAmount(payment.getTaxAmount());

        String signature = signatureService.generateSignature(
                totalAmountForEsewa,
                transactionUuid,
                esewaConfig.getProductCode(),
                esewaConfig.getSecretKey()
        );

        return CreatePaymentResponse.builder()
                .paymentUrl(esewaConfig.getPaymentUrl())
                .transactionUuid(transactionUuid)
                .productCode(esewaConfig.getProductCode())
                .amount(amountForEsewa)
                .taxAmount(taxAmountForEsewa)
                .totalAmount(totalAmountForEsewa)
                .signedFieldNames(SIGNED_FIELD_NAMES)
                .signature(signature)
                .successUrl(esewaConfig.getBackendSuccessUrl())
                .failureUrl(esewaConfig.getBackendFailureUrl())
                .productServiceCharge("0")
                .productDeliveryCharge("0")
                .currency("NPR")
                .paymentId(payment.getId())
                .amountValue(payment.getAmount())
                .totalAmountValue(payment.getTotalAmount())
                .build();
    }

    // ===== Verification =====

    private EsewaStatusResult verifyWithEsewa(Payment payment) {
        String statusUrl = esewaConfig.getStatusUrl();
        // eSewa expects amounts as integers (e.g. "499" not "499.00")
        String totalAmountForEsewa = toEsewaAmount(payment.getTotalAmount());
        String url = String.format("%s?product_code=%s&total_amount=%s&transaction_uuid=%s",
                statusUrl,
                esewaConfig.getProductCode(),
                totalAmountForEsewa,
                payment.getTransactionUuid());

        log.info("Verifying payment with eSewa status API: transaction_uuid={}", payment.getTransactionUuid());

        try {
            RestClient restClient = RestClient.create();
            ResponseEntity<Map> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new PaymentException("Empty response from eSewa status API");
            }

            // eSewa status API returns snake_case field names:
            // { "product_code": "...", "transaction_uuid": "...", "total_amount": 100.0, "status": "...", "ref_id": "..." }
            String status = (String) body.get("status");
            Object refIdObj = body.get("ref_id") != null ? body.get("ref_id") : body.get("refId");
            String refId = refIdObj != null ? String.valueOf(refIdObj) : null;
            Object totalAmountObj = body.get("total_amount") != null ? body.get("total_amount") : body.get("totalAmount");
            String totalAmountStr = totalAmountObj != null ? String.valueOf(totalAmountObj) : null;
            Object productCodeObj = body.get("product_code") != null ? body.get("product_code") : body.get("productCode");
            String productCode = productCodeObj != null ? String.valueOf(productCodeObj) : esewaConfig.getProductCode();

            log.info("eSewa status response: status={}, ref_id={}", status, refId);

            return new EsewaStatusResult(status, refId, totalAmountStr, productCode);
        } catch (Exception e) {
            log.error("Failed to verify payment with eSewa: {}", e.getMessage(), e);
            throw new PaymentException("Failed to verify payment with eSewa: " + e.getMessage());
        }
    }

    // ===== Subscription Activation =====

    private void activateSubscription(Payment payment) {
        SubscriptionPlan plan = planService.getPlanEntity(payment.getSubscriptionPlanId());

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = calculateEndDate(startDate, payment.getBillingCycle());

        // Snapshot plan details so later admin changes don't affect this subscription
        // The plan's maxVehicles is a MONTHLY limit. For longer billing cycles,
        // multiply by the number of months to get the total vehicle limit for the
        // entire subscription period.
        // Example: monthly limit 10 + yearly cycle → total limit = 10 × 12 = 120
        Integer monthlyVehicleLimit = null;
        if (plan.getRestrictions() != null) {
            for (SubscriptionPlanRestriction r : plan.getRestrictions()) {
                if (r.getMaxVehicles() != null) {
                    monthlyVehicleLimit = r.getMaxVehicles();
                    break;
                }
            }
        }
        Integer vehicleLimit = monthlyVehicleLimit != null
                ? monthlyVehicleLimit * getMonthsInCycle(payment.getBillingCycle())
                : null;

        Subscription subscription = Subscription.builder()
                .shopOwnerId(payment.getShopOwnerId())
                .plan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .renewalDate(endDate)
                .status(SubscriptionStatus.ACTIVE)
                .autoRenewal(false)
                // Snapshot plan details at purchase time
                .planNameSnapshot(plan.getName())
                .planDescriptionSnapshot(plan.getShortDescription() != null ? plan.getShortDescription() : plan.getDescription())
                .planIconSnapshot(plan.getIcon())
                .planThemeColorSnapshot(plan.getThemeColor())
                .vehicleLimitSnapshot(vehicleLimit)
                .pricePaid(payment.getAmount())
                .billingCycleSnapshot(payment.getBillingCycle())
                .build();

        subscription = subscriptionRepository.save(subscription);
        payment.setSubscriptionId(subscription.getId());
        paymentRepository.save(payment);
        log.info("Subscription {} activated for shop_owner={}", subscription.getId(), payment.getShopOwnerId());

        // Create SubscriptionTransaction record for Super Admin transactions page
        createTransactionRecord(payment, plan, subscription.getId());

        // Send payment success email with invoice details
        paymentEmailService.sendPaymentSuccessEmail(payment, plan);

        // Auto-create an expense record so it shows in the expenses list
        paymentExpenseSyncService.createSubscriptionExpense(payment, plan);
    }

    private void createTransactionRecord(Payment payment, SubscriptionPlan plan, Long subscriptionId) {
        SubscriptionTransaction transaction = SubscriptionTransaction.builder()
                .transactionId(payment.getTransactionUuid())
                .subscriptionId(subscriptionId)
                .shopOwnerId(payment.getShopOwnerId())
                .plan(plan)
                .amount(payment.getAmount())
                .tax(payment.getTaxAmount())
                .discount(BigDecimal.ZERO)
                .finalAmount(payment.getTotalAmount())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "ESEWA")
                .gateway(payment.getGateway())
                .status(TransactionStatus.COMPLETED)
                .invoiceNumber(payment.getInvoiceNumber())
                .transactionDate(payment.getPaidAt() != null ? payment.getPaidAt() : LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);
        log.info("SubscriptionTransaction created for payment {}", payment.getTransactionUuid());
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
     * Example: yearly → 12, quarterly → 3, monthly → 1
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

    // ===== Helpers =====

    /**
     * Converts a BigDecimal amount to the integer string format eSewa expects.
     * eSewa ePay V2 requires amounts without decimal places (e.g. "499" not "499.00").
     * NPR amounts are typically whole rupees.
     */
    private String toEsewaAmount(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal getPriceForCycle(SubscriptionPlan plan, String billingCycle) {
        Set<SubscriptionPlanPricing> pricings = plan.getPricings();
        if (pricings == null || pricings.isEmpty()) {
            throw new PaymentException("No pricing configured for this plan");
        }
        SubscriptionPlanPricing pricing = pricings.iterator().next();
        switch (billingCycle.toLowerCase()) {
            case "monthly": return pricing.getMonthly();
            case "quarterly": return pricing.getQuarterly();
            case "halfyearly":
            case "half_yearly": return pricing.getHalfYearly();
            case "yearly": return pricing.getYearly();
            default: throw new PaymentException("Invalid billing cycle: " + billingCycle);
        }
    }

    private String generateTransactionUuid() {
        String datePart = LocalDateTime.now().format(TXN_UUID_FMT);
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        return String.format("SS-%s-%06d", datePart, random);
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
                .discountAmount(payment.getDiscountAmount())
                .couponCode(payment.getCouponCodeSnapshot())
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

    // Internal class for eSewa status response
    private record EsewaStatusResult(String status, String refId, String totalAmount, String productCode) {}
}
