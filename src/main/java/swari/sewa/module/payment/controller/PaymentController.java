package swari.sewa.module.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.module.payment.config.EsewaConfig;
import swari.sewa.module.payment.config.FonepayConfig;
import swari.sewa.module.payment.dto.CreatePaymentRequest;
import swari.sewa.module.payment.dto.CreatePaymentResponse;
import swari.sewa.module.payment.dto.CurrentSubscriptionResponse;
import swari.sewa.module.payment.dto.BillingHistoryItem;
import swari.sewa.module.payment.dto.PaymentResponse;
import swari.sewa.module.payment.exception.PaymentException;
import swari.sewa.common.util.HtmlEscape;
import swari.sewa.module.payment.service.EsewaPaymentService;
import swari.sewa.module.payment.service.FinanceAuditService;
import swari.sewa.module.payment.service.FonepayPaymentService;
import swari.sewa.module.payment.service.InvoiceAccessTokenService;
import swari.sewa.module.subscription.service.SubscriptionSettingsService;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final EsewaPaymentService esewaPaymentService;
    private final FonepayPaymentService fonepayPaymentService;
    private final ShopOwnerRepository shopOwnerRepository;
    private final EsewaConfig esewaConfig;
    private final FonepayConfig fonepayConfig;
    private final swari.sewa.module.subscription.repository.SubscriptionRepository subscriptionRepository;
    private final swari.sewa.module.payment.repository.PaymentRepository paymentRepository;
    private final swari.sewa.module.subscription.service.SubscriptionPlanService planService;
    private final swari.sewa.common.util.JwtUtil jwtUtil;
    private final swari.sewa.module.vehicle.repository.VehicleRepository vehicleRepository;
    private final swari.sewa.module.enquiry.repository.EnquiryRepository enquiryRepository;
    private final SubscriptionSettingsService settingsService;
    private final InvoiceAccessTokenService invoiceAccessTokenService;
    private final FinanceAuditService financeAuditService;

    /**
     * QR code payment page — renders an auto-submitting HTML form to eSewa.
     * The QR code encodes this URL. When scanned with a phone camera,
     * the browser opens this page, which auto-submits to eSewa.
     * No authentication required — the payment is located by transaction UUID.
     */
    @GetMapping("/esewa/pay/{transactionUuid}")
    public void qrPayPage(
            @PathVariable String transactionUuid,
            HttpServletResponse httpServletResponse) throws IOException {

        log.info("QR pay page requested for transaction_uuid={}", transactionUuid);

        try {
            CreatePaymentResponse data = esewaPaymentService.getEsewaFormParams(transactionUuid);

            // Render a minimal HTML page with an auto-submitting form
            String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>eSewa Payment</title>
                    <style>
                        body { font-family: sans-serif; text-align: center; padding: 40px 20px; background: #f8fafc; }
                        .container { max-width: 400px; margin: 0 auto; background: #fff; border-radius: 16px; padding: 30px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                        .logo { width: 60px; height: 60px; border-radius: 12px; background: #60a5fa; color: #fff; font-size: 28px; font-weight: 700; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 16px; }
                        h2 { color: #1f2937; margin: 0 0 8px 0; }
                        p { color: #6b7280; margin: 0 0 20px 0; font-size: 14px; }
                        .amount { font-size: 28px; font-weight: 700; color: #f97316; margin: 12px 0; }
                        .spinner { width: 32px; height: 32px; border: 3px solid #e5e7eb; border-top-color: #60a5fa; border-radius: 50%%; animation: spin 0.8s linear infinite; margin: 20px auto; }
                        @keyframes spin { to { transform: rotate(360deg); } }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="logo">e</div>
                        <h2>Redirecting to eSewa</h2>
                        <p>You are being redirected to eSewa to complete your payment</p>
                        <div class="amount">Rs. %s</div>
                        <div class="spinner"></div>
                        <p style="font-size:12px; color:#9ca3af;">Transaction: %s</p>
                    </div>
                    <form id="esewaForm" action="%s" method="POST">
                        <input type="hidden" name="amount" value="%s">
                        <input type="hidden" name="tax_amount" value="%s">
                        <input type="hidden" name="total_amount" value="%s">
                        <input type="hidden" name="transaction_uuid" value="%s">
                        <input type="hidden" name="product_code" value="%s">
                        <input type="hidden" name="product_service_charge" value="%s">
                        <input type="hidden" name="product_delivery_charge" value="%s">
                        <input type="hidden" name="success_url" value="%s">
                        <input type="hidden" name="failure_url" value="%s">
                        <input type="hidden" name="signed_field_names" value="%s">
                        <input type="hidden" name="signature" value="%s">
                    </form>
                    <script>document.getElementById('esewaForm').submit();</script>
                </body>
                </html>
                """,
                data.getTotalAmount(),
                transactionUuid,
                data.getPaymentUrl(),
                data.getAmount(),
                data.getTaxAmount(),
                data.getTotalAmount(),
                data.getTransactionUuid(),
                data.getProductCode(),
                data.getProductServiceCharge(),
                data.getProductDeliveryCharge(),
                data.getSuccessUrl(),
                data.getFailureUrl(),
                data.getSignedFieldNames(),
                data.getSignature()
            );

            httpServletResponse.setContentType("text/html;charset=UTF-8");
            httpServletResponse.getWriter().write(html);
        } catch (Exception e) {
            log.error("Failed to generate QR pay page for {}: {}", transactionUuid, e.getMessage());
            httpServletResponse.sendRedirect(esewaConfig.getFrontendFailureUrl() + "?error=qr_payment_failed");
        }
    }

    /**
     * Create a new eSewa payment.
     * Authenticates the shop owner, fetches the plan, creates a PENDING payment,
     * and returns all parameters needed to submit the eSewa form.
     */
    @PostMapping("/esewa/create")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Long shopOwnerId = getCurrentShopOwnerId();
        if (shopOwnerId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        CreatePaymentResponse response = esewaPaymentService.createPayment(request, shopOwnerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment initiated successfully"));
    }

    /**
     * eSewa success callback — eSewa redirects the user's browser here directly.
     *
     * This endpoint:
     * 1. Decodes the Base64 response data from eSewa
     * 2. Calls the backend service to verify the payment with eSewa's status API
     * 3. Activates the subscription and generates invoice (if verification passes)
     * 4. Redirects the user to the frontend result page with the transaction UUID
     *
     * Payment verification does NOT depend on React/JavaScript — the backend is
     * the source of truth.
     */
    @GetMapping("/esewa/success")
    public void paymentSuccess(
            HttpServletResponse httpServletResponse,
            @RequestParam(value = "data", required = false) String data,
            @RequestParam(value = "transaction_uuid", required = false) String transactionUuid,
            @RequestParam(value = "total_amount", required = false) String totalAmount,
            @RequestParam(value = "transaction_code", required = false) String transactionCode,
            @RequestParam(value = "refId", required = false) String refId,
            @RequestParam(value = "status", required = false) String status) throws IOException {

        log.info("eSewa success callback received: data={}, transaction_uuid={}, status={}",
                data != null ? "[present]" : "null", transactionUuid, status);

        // eSewa V2 sends data as Base64-encoded JSON in the "data" parameter
        if (data != null && !data.isEmpty()) {
            try {
                String decoded = new String(Base64.getDecoder().decode(data));
                log.debug("Decoded eSewa response (not logging sensitive data)");
                transactionUuid = extractJsonField(decoded, "transaction_uuid");
                totalAmount = extractJsonField(decoded, "total_amount");
                transactionCode = extractJsonField(decoded, "transaction_code");
                status = extractJsonField(decoded, "status");
            } catch (Exception e) {
                log.error("Failed to decode eSewa response data", e);
            }
        }

        if (transactionUuid == null || transactionUuid.isEmpty()) {
            log.error("Missing transaction_uuid in eSewa success callback");
            httpServletResponse.sendRedirect(esewaConfig.getFrontendFailureUrl() + "?error=missing_transaction_uuid");
            return;
        }

        // Verify payment and activate subscription — backend is source of truth
        PaymentResponse response = esewaPaymentService.handleSuccessCallback(
                transactionUuid, totalAmount, transactionCode, refId, status);

        // Redirect to frontend result page with transaction UUID
        String redirectUrl = esewaConfig.getFrontendSuccessUrl() + "?transaction_uuid=" + transactionUuid;
        log.info("Redirecting to frontend: {} after payment verification (status={})", redirectUrl, response.getStatus());
        httpServletResponse.sendRedirect(redirectUrl);
    }

    /**
     * eSewa failure callback — eSewa redirects the user's browser here directly.
     *
     * Marks the payment as FAILED and does NOT activate the subscription.
     * Then redirects to the frontend failure page.
     */
    @GetMapping("/esewa/failure")
    public void paymentFailure(
            HttpServletResponse httpServletResponse,
            @RequestParam(value = "data", required = false) String data,
            @RequestParam(value = "transaction_uuid", required = false) String transactionUuid,
            @RequestParam(value = "reason", required = false) String reason) throws IOException {

        log.info("eSewa failure callback received: data={}, transaction_uuid={}, reason={}",
                data != null ? "[present]" : "null", transactionUuid, reason);

        // Try to decode Base64 data if present
        if (data != null && !data.isEmpty()) {
            try {
                String decoded = new String(Base64.getDecoder().decode(data));
                log.info("Decoded eSewa failure data: {}", decoded);
                transactionUuid = extractJsonField(decoded, "transaction_uuid");
            } catch (Exception e) {
                log.error("Failed to decode eSewa failure data", e);
            }
        }

        if (transactionUuid == null || transactionUuid.isEmpty()) {
            log.error("Missing transaction_uuid in eSewa failure callback");
            httpServletResponse.sendRedirect(esewaConfig.getFrontendFailureUrl() + "?error=missing_transaction_uuid");
            return;
        }

        esewaPaymentService.handleFailureCallback(transactionUuid, reason);

        String redirectUrl = esewaConfig.getFrontendFailureUrl() + "?transaction_uuid=" + transactionUuid;
        log.info("Redirecting to frontend failure page: {}", redirectUrl);
        httpServletResponse.sendRedirect(redirectUrl);
    }

    /**
     * Get payment status by transaction UUID.
     * Used by the frontend result pages to display the verified payment status.
     * Requires authentication — a shop owner can only view their own payments.
     */
    @GetMapping("/esewa/status/{transactionUuid}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(@PathVariable String transactionUuid) {
        Long shopOwnerId = getCurrentShopOwnerId();
        if (shopOwnerId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        PaymentResponse response = esewaPaymentService.getPaymentByTransactionUuid(transactionUuid);

        // Security: shop owner can only view their own payments
        if (!response.getShopOwnerId().equals(shopOwnerId)) {
            log.warn("Shop owner {} attempted to view payment {} belonging to shop owner {}",
                    shopOwnerId, transactionUuid, response.getShopOwnerId());
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success(response, "Payment status retrieved"));
    }

    // ===== Helpers =====

    private Long getCurrentShopOwnerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return shopOwnerRepository.findByEmail(auth.getName())
                .map(ShopOwner::getId)
                .orElse(null);
    }

    /**
     * Calculate an approximate end date from a start date and billing cycle.
     * Used as a fallback for legacy payments that don't have date snapshots.
     */
    private java.time.LocalDateTime calculateApproximateEndDate(java.time.LocalDateTime startDate, String billingCycle) {
        if (billingCycle == null) return startDate.plusMonths(1);
        switch (billingCycle.toLowerCase()) {
            case "monthly": return startDate.plusMonths(1);
            case "quarterly": return startDate.plusMonths(3);
            case "half-yearly": case "half_yearly": return startDate.plusMonths(6);
            case "yearly": return startDate.plusMonths(12);
            default: return startDate.plusMonths(1);
        }
    }

    /**
     * Simple JSON field extractor — avoids adding Jackson parsing for the eSewa response.
     */
    private String extractJsonField(String json, String fieldName) {
        String search = "\"" + fieldName + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colonIdx = json.indexOf(":", idx + search.length());
        if (colonIdx == -1) return null;
        int startQuote = json.indexOf("\"", colonIdx + 1);
        if (startQuote == -1) return null;
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) return null;
        return json.substring(startQuote + 1, endQuote);
    }

    // ===== Fonepay Endpoints =====

    /**
     * Create a Fonepay payment — returns the signed redirect URL.
     * The frontend redirects the user to this URL, where Fonepay shows a QR code
     * that the user can scan with eSewa, Khalti, IME Pay, or any Fonepay-compatible app.
     */
    @PostMapping("/fonepay/create")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> createFonepayPayment(
            @Valid @RequestBody CreatePaymentRequest request) {

        Long shopOwnerId = getCurrentShopOwnerId();
        if (shopOwnerId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        log.info("Fonepay payment creation requested by shop_owner={} for plan={}", shopOwnerId, request.getPlanId());

        String redirectUrl = fonepayPaymentService.createPayment(request, shopOwnerId);

        return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of("redirectUrl", redirectUrl),
                "Fonepay payment initiated"
        ));
    }

    /**
     * Fonepay callback — Fonepay redirects the user's browser here after payment.
     * Verifies the signature, activates subscription, then redirects to frontend.
     */
    @GetMapping("/fonepay/verify")
    public void fonepayCallback(
            HttpServletResponse httpServletResponse,
            @RequestParam("PRN") String prn,
            @RequestParam("PID") String pid,
            @RequestParam("PS") String ps,
            @RequestParam("AMT") String amt,
            @RequestParam("UID") String uid,
            @RequestParam("BID") String bid,
            @RequestParam("DV") String dv) throws IOException {

        log.info("Fonepay callback: prn={}, ps={}", prn, ps);

        try {
            PaymentResponse response = fonepayPaymentService.handleCallback(prn, pid, ps, amt, uid, bid, dv);

            String redirectUrl;
            if ("SUCCESS".equals(response.getStatus())) {
                redirectUrl = fonepayConfig.getFrontendSuccessUrl() + "?transaction_uuid=" + prn;
            } else {
                redirectUrl = fonepayConfig.getFrontendFailureUrl() + "?transaction_uuid=" + prn;
            }
            log.info("Redirecting to frontend: {} (status={})", redirectUrl, response.getStatus());
            httpServletResponse.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("Fonepay callback error: {}", e.getMessage(), e);
            httpServletResponse.sendRedirect(fonepayConfig.getFrontendFailureUrl() + "?error=fonepay_callback_failed");
        }
    }

    /**
     * Get Fonepay payment status by PRN.
     * Used by the frontend result pages.
     */
    @GetMapping("/fonepay/status/{prn}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getFonepayPaymentStatus(@PathVariable String prn) {
        Long shopOwnerId = getCurrentShopOwnerId();
        if (shopOwnerId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        PaymentResponse response = fonepayPaymentService.getPaymentByPrn(prn);

        if (!response.getShopOwnerId().equals(shopOwnerId)) {
            log.warn("Shop owner {} attempted to view payment {} belonging to shop owner {}",
                    shopOwnerId, prn, response.getShopOwnerId());
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success(response, "Payment status retrieved"));
    }

    // ===== Current Subscription Endpoint =====

    /**
     * Get the current shop owner's subscription with plan details, pricing, and usage limits.
     * Replaces dummy data on the shop owner subscription dashboard.
     */
    @GetMapping("/subscription/current")
    public ResponseEntity<ApiResponse<CurrentSubscriptionResponse>> getCurrentSubscription() {
        Long shopOwnerId = getCurrentShopOwnerId();
        if (shopOwnerId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        log.info("Fetching current subscription for shop_owner={}", shopOwnerId);

        // Find the latest ACTIVE or TRIAL subscription
        var subscriptions = subscriptionRepository.findByShopOwnerIdAndStatus(
                shopOwnerId,
                swari.sewa.module.subscription.enums.SubscriptionStatus.ACTIVE
        );
        if (subscriptions.isEmpty()) {
            // No ACTIVE subscription — check for TRIAL
            subscriptions = subscriptionRepository.findByShopOwnerIdAndStatus(
                    shopOwnerId,
                    swari.sewa.module.subscription.enums.SubscriptionStatus.TRIAL
            );
        }

        if (subscriptions.isEmpty()) {
            // No active or trial subscription — return empty response
            return ResponseEntity.ok(ApiResponse.success(null, "No active subscription"));
        }

        var subscription = subscriptions.get(0);
        var plan = subscription.getPlan();

        // Find the successful payment for this subscription to get billing cycle and price
        var payments = paymentRepository.findByShopOwnerIdAndStatusOrderByPaidAtDescIdDesc(
                shopOwnerId,
                swari.sewa.module.payment.enums.PaymentStatus.SUCCESS
        );

        String billingCycle = "monthly";
        java.math.BigDecimal price = java.math.BigDecimal.ZERO;
        String invoiceNumber = null;
        String gateway = null;
        String transactionUuid = null;

        if (!payments.isEmpty()) {
            var latestPayment = payments.get(0);
            billingCycle = latestPayment.getBillingCycle() != null ? latestPayment.getBillingCycle() : "monthly";
            price = latestPayment.getTotalAmount() != null ? latestPayment.getTotalAmount() : java.math.BigDecimal.ZERO;
            invoiceNumber = latestPayment.getInvoiceNumber();
            gateway = latestPayment.getGateway();
            transactionUuid = latestPayment.getTransactionUuid();
        }

        // Calculate days until expiry
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        int daysUntilExpiry = 0;
        if (subscription.getEndDate() != null) {
            daysUntilExpiry = (int) java.time.Duration.between(now, subscription.getEndDate()).toDays();
            if (daysUntilExpiry < 0) daysUntilExpiry = 0;
        }

        // Get plan limits — prefer snapshotted values (frozen at purchase time)
        // so admin changes to the plan don't affect existing subscriptions
        Integer vehicleLimit = subscription.getVehicleLimitSnapshot();
        Integer storageLimit = null;
        String enquiryLimit = "Unlimited";
        Integer featuredLimit = null;

        // Fall back to plan restrictions only if snapshot is null (legacy subscriptions)
        if (vehicleLimit == null && plan.getRestrictions() != null) {
            for (var restriction : plan.getRestrictions()) {
                if (restriction.getMaxVehicles() != null) vehicleLimit = restriction.getMaxVehicles();
                if (restriction.getMaxStorage() != null) {
                    try { storageLimit = Integer.parseInt(restriction.getMaxStorage()); } catch (Exception e) { }
                }
            }
        }

        // Fetch actual usage counts from the database
        // Count vehicles added in the current billing period (using currentPeriodStart,
        // not the original startDate, so renewals/upgrades get a fresh count)
        java.time.LocalDateTime periodStart = subscription.getCurrentPeriodStart() != null
                ? subscription.getCurrentPeriodStart()
                : subscription.getStartDate();
        int vehiclesUsed = (int) vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtAfter(
                shopOwnerId, periodStart);
        int enquiriesUsed = (int) enquiryRepository.countByShop_ShopOwner_Id(shopOwnerId);
        int featuredUsed = (int) vehicleRepository.countFeaturedByShop_ShopOwner_Id(shopOwnerId);

        // Use snapshotted plan details (frozen at purchase time), fall back to live plan for legacy
        String snapshotPlanName = subscription.getPlanNameSnapshot() != null ? subscription.getPlanNameSnapshot() : plan.getName();
        String snapshotPlanDesc = subscription.getPlanDescriptionSnapshot() != null ? subscription.getPlanDescriptionSnapshot()
                : (plan.getShortDescription() != null ? plan.getShortDescription() : plan.getDescription());
        String snapshotIcon = subscription.getPlanIconSnapshot() != null ? subscription.getPlanIconSnapshot() : plan.getIcon();
        String snapshotThemeColor = subscription.getPlanThemeColorSnapshot() != null ? subscription.getPlanThemeColorSnapshot() : plan.getThemeColor();

        // Extract previous plan info from the second-most-recent SUCCESSFUL payment.
        // Each successful payment has immutable snapshot fields (planNameSnapshot,
        // subscriptionStartDateSnapshot, subscriptionEndDateSnapshot) that were
        // frozen at activation time. Unlike the Subscription entity (which is
        // reused and overwritten on renewal/upgrade), these Payment-level
        // snapshots are historically immutable.
        String previousPlanName = null;
        java.math.BigDecimal previousPlanPrice = null;
        String previousPlanBillingCycle = null;
        java.time.LocalDateTime previousPlanStartDate = null;
        java.time.LocalDateTime previousPlanEndDate = null;
        Integer previousPlanVehicleLimit = null;
        Integer previousPlanVehiclesUsed = null;
        Integer previousPlanUnused = null;
        boolean hasPreviousPlan = false;

        if (payments.size() >= 2) {
            var previousPayment = payments.get(1);
            // Use the Payment's own snapshot (frozen at activation time)
            previousPlanName = previousPayment.getPlanNameSnapshot();
            // Fall back to live plan lookup only if snapshot is null (legacy payments)
            if (previousPlanName == null) {
                try {
                    var prevPlan = planService.getPlanEntity(previousPayment.getSubscriptionPlanId());
                    if (prevPlan != null) previousPlanName = prevPlan.getName();
                } catch (Exception e) { /* plan may be deleted */ }
            }
            if (previousPlanName != null) {
                previousPlanPrice = previousPayment.getTotalAmount();
                previousPlanBillingCycle = previousPayment.getBillingCycle();
                previousPlanStartDate = previousPayment.getSubscriptionStartDateSnapshot();
                previousPlanEndDate = previousPayment.getSubscriptionEndDateSnapshot();
                previousPlanVehicleLimit = previousPayment.getVehicleLimitSnapshot();
                // Fallback for legacy payments (created before V54 snapshot columns):
                // use paidAt as approximate start, and calculate end from billing cycle
                if (previousPlanStartDate == null && previousPayment.getPaidAt() != null) {
                    previousPlanStartDate = previousPayment.getPaidAt();
                }
                if (previousPlanEndDate == null && previousPlanStartDate != null && previousPlanBillingCycle != null) {
                    previousPlanEndDate = calculateApproximateEndDate(previousPlanStartDate, previousPlanBillingCycle);
                }
                // Calculate vehicles used during the previous period
                if (previousPlanStartDate != null) {
                    java.time.LocalDateTime prevEnd = previousPlanEndDate != null ? previousPlanEndDate : java.time.LocalDateTime.now();
                    previousPlanVehiclesUsed = (int) vehicleRepository.countByShop_ShopOwner_IdAndCreatedAtBetween(
                            shopOwnerId, previousPlanStartDate, prevEnd);
                    if (previousPlanVehicleLimit != null) {
                        previousPlanUnused = Math.max(0, previousPlanVehicleLimit - previousPlanVehiclesUsed);
                    }
                }
                hasPreviousPlan = true;
            }
        }

        // Rollover fields from the subscription
        Integer newPlanVehicleLimit = subscription.getNewPlanVehicleLimit();
        Integer carriedForwardVehicleLimit = subscription.getCarriedForwardVehicleLimit();
        Integer totalVehicleLimit = vehicleLimit;
        Integer vehiclesRemaining = vehicleLimit != null ? Math.max(0, vehicleLimit - vehiclesUsed) : null;

        CurrentSubscriptionResponse response = CurrentSubscriptionResponse.builder()
                .subscriptionId(subscription.getId())
                .status(subscription.getStatus().name())
                .planId(plan.getId())
                .planName(snapshotPlanName)
                .planDescription(snapshotPlanDesc)
                .icon(snapshotIcon)
                .themeColor(snapshotThemeColor)
                .price(price)
                .billingCycle(billingCycle)
                .currency("NPR")
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .renewalDate(subscription.getRenewalDate())
                .daysUntilExpiry(daysUntilExpiry)
                .vehicleLimit(vehicleLimit)
                .storageLimit(storageLimit)
                .enquiryLimit(enquiryLimit)
                .featuredLimit(featuredLimit)
                .vehiclesUsed(vehiclesUsed)
                .enquiriesUsed(enquiriesUsed)
                .featuredUsed(featuredUsed)
                .newPlanVehicleLimit(newPlanVehicleLimit)
                .carriedForwardVehicleLimit(carriedForwardVehicleLimit)
                .totalVehicleLimit(totalVehicleLimit)
                .vehiclesRemaining(vehiclesRemaining)
                .invoiceNumber(invoiceNumber)
                .paymentGateway(gateway)
                .transactionUuid(transactionUuid)
                .previousPlanName(previousPlanName)
                .previousPlanPrice(previousPlanPrice)
                .previousPlanBillingCycle(previousPlanBillingCycle)
                .previousPlanStartDate(previousPlanStartDate)
                .previousPlanEndDate(previousPlanEndDate)
                .previousPlanVehicleLimit(previousPlanVehicleLimit)
                .previousPlanVehiclesUsed(previousPlanVehiclesUsed)
                .previousPlanUnused(previousPlanUnused)
                .hasPreviousPlan(hasPreviousPlan)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Current subscription retrieved"));
    }

    // ===== Billing History =====

    /**
     * Get billing history (all payments) for the authenticated shop owner.
     * Only successful payments with invoices are included.
     */
    @GetMapping("/billing-history")
    public ResponseEntity<ApiResponse<java.util.List<BillingHistoryItem>>> getBillingHistory() {
        Long shopOwnerId = getCurrentShopOwnerId();
        if (shopOwnerId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        log.info("Fetching billing history for shop_owner={}", shopOwnerId);

        var payments = paymentRepository.findByShopOwnerIdAndStatusOrderByPaidAtDescIdDesc(
                shopOwnerId,
                swari.sewa.module.payment.enums.PaymentStatus.SUCCESS
        );

        // ── Batch-fetch subscriptions and plans for legacy payments to avoid N+1 ──
        // Before: for each payment without planNameSnapshot, 1-2 queries per payment
        //         (subscriptionRepository.findById + planService.getPlanEntity)
        // After: collect all needed subscription IDs and plan IDs, batch-fetch in 2 queries,
        //        then look up in-memory maps.
        java.util.Set<Long> subscriptionIdsNeeded = new java.util.HashSet<>();
        java.util.Set<Long> planIdsNeeded = new java.util.HashSet<>();
        for (var payment : payments) {
            if (payment.getPlanNameSnapshot() == null) {
                if (payment.getSubscriptionId() != null) subscriptionIdsNeeded.add(payment.getSubscriptionId());
                if (payment.getSubscriptionPlanId() != null) planIdsNeeded.add(payment.getSubscriptionPlanId());
            }
        }

        // Batch-fetch subscriptions
        Map<Long, String> subscriptionPlanNames = new java.util.HashMap<>();
        if (!subscriptionIdsNeeded.isEmpty()) {
            for (var sub : subscriptionRepository.findAllById(subscriptionIdsNeeded)) {
                if (sub.getPlanNameSnapshot() != null) {
                    subscriptionPlanNames.put(sub.getId(), sub.getPlanNameSnapshot());
                }
            }
        }

        // Batch-fetch plans
        Map<Long, String> planNames = new java.util.HashMap<>();
        if (!planIdsNeeded.isEmpty()) {
            for (var plan : planService.findAllById(planIdsNeeded)) {
                planNames.put(plan.getId(), plan.getName());
            }
        }

        java.util.List<BillingHistoryItem> items = new java.util.ArrayList<>();
        for (var payment : payments) {
            String planName = "—";
            // Prefer the Payment's own plan name snapshot (frozen at activation time,
            // immutable across renewals/upgrades)
            if (payment.getPlanNameSnapshot() != null) {
                planName = payment.getPlanNameSnapshot();
            }
            // Fall back to subscription snapshot (legacy payments without planNameSnapshot)
            else if (payment.getSubscriptionId() != null && subscriptionPlanNames.containsKey(payment.getSubscriptionId())) {
                planName = subscriptionPlanNames.get(payment.getSubscriptionId());
            }
            // Fall back to live plan only if no snapshot at all
            else if (payment.getSubscriptionPlanId() != null && planNames.containsKey(payment.getSubscriptionPlanId())) {
                planName = planNames.get(payment.getSubscriptionPlanId());
            }

            items.add(BillingHistoryItem.builder()
                    .id(payment.getId())
                    .invoiceNumber(payment.getInvoiceNumber())
                    .transactionUuid(payment.getTransactionUuid())
                    .gateway(payment.getGateway())
                    .status(payment.getStatus().name())
                    .billingCycle(payment.getBillingCycle())
                    .amount(payment.getAmount())
                    .discountAmount(payment.getDiscountAmount())
                    .couponCode(payment.getCouponCodeSnapshot())
                    .taxAmount(payment.getTaxAmount())
                    .totalAmount(payment.getTotalAmount())
                    .paidAt(payment.getPaidAt())
                    .planName(planName)
                    .subscriptionId(payment.getSubscriptionId())
                    .build());
        }

        return ResponseEntity.ok(ApiResponse.success(items, "Billing history retrieved"));
    }

    // ===== Invoice Download =====

    /**
     * Mint a short-lived, single-use token that authorises opening ONE invoice.
     *
     * <p>Called with the normal {@code Authorization} header. The returned token is
     * what gets placed in the invoice URL, so the caller's real JWT never appears
     * in browser history, referrer headers or access logs.
     *
     * <p>Ownership is validated here at issue time AND again at redeem time.
     */
    @PostMapping("/invoice/{transactionUuid}/access-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createInvoiceAccessToken(
            @PathVariable String transactionUuid) {

        Long shopOwnerId = getCurrentShopOwnerId();
        boolean isSuperAdmin = isCurrentUserSuperAdmin(null);

        if (shopOwnerId == null && !isSuperAdmin) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        var payment = paymentRepository.findByTransactionUuid(transactionUuid).orElse(null);
        if (payment == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Invoice not found"));
        }
        // Do not let the transaction UUID itself act as authorisation.
        if (!isSuperAdmin && !payment.getShopOwnerId().equals(shopOwnerId)) {
            log.warn("Shop owner {} requested an invoice token for {} owned by shop owner {} — denied",
                    shopOwnerId, transactionUuid, payment.getShopOwnerId());
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        if (payment.getStatus() != swari.sewa.module.payment.enums.PaymentStatus.SUCCESS) {
            return ResponseEntity.status(409).body(ApiResponse.error("Invoice not available for this payment status"));
        }

        var subjectType = isSuperAdmin
                ? InvoiceAccessTokenService.SubjectType.SUPERADMIN
                : InvoiceAccessTokenService.SubjectType.SHOP_OWNER;
        String accessToken = invoiceAccessTokenService.issue(transactionUuid, subjectType, shopOwnerId);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("accessToken", accessToken, "expiresInSeconds", invoiceAccessTokenService.ttlSeconds()),
                "Invoice access token issued"));
    }

    /**
     * Render a printable invoice.
     *
     * <p>Authorisation accepts either:
     * <ul>
     *   <li>a normal authenticated session (Authorization header), or</li>
     *   <li>an {@code accessToken} minted by
     *       {@link #createInvoiceAccessToken(String)}, which is scoped to this one
     *       invoice, single-use and short-lived.</li>
     * </ul>
     *
     * <p>A full JWT is deliberately NOT accepted as a query parameter.
     */
    @GetMapping("/invoice/{transactionUuid}")
    public void downloadInvoice(
            @PathVariable String transactionUuid,
            @RequestParam(value = "accessToken", required = false) String accessToken,
            jakarta.servlet.http.HttpServletResponse httpServletResponse) throws IOException {

        Long shopOwnerId = getCurrentShopOwnerId();
        boolean isSuperAdmin = isCurrentUserSuperAdmin(null);

        // No header-based session (browser tab navigation) — fall back to the
        // scoped invoice token. It is bound to this exact transactionUuid, so it
        // cannot be replayed against a different invoice.
        if (shopOwnerId == null && !isSuperAdmin) {
            var access = invoiceAccessTokenService.redeem(accessToken, transactionUuid);
            if (access == null) {
                httpServletResponse.sendError(401, "Authentication required");
                return;
            }
            isSuperAdmin = access.subjectType() == InvoiceAccessTokenService.SubjectType.SUPERADMIN;
            shopOwnerId = access.subjectId();
        }

        if (shopOwnerId == null && !isSuperAdmin) {
            httpServletResponse.sendError(401, "Authentication required");
            return;
        }

        log.info("Invoice render requested for transaction_uuid={} by shop_owner={} superAdmin={}",
                transactionUuid, shopOwnerId, isSuperAdmin);

        try {
            // Find the payment
            swari.sewa.module.payment.entity.Payment payment = paymentRepository.findByTransactionUuid(transactionUuid)
                    .orElse(null);
            if (payment == null) {
                httpServletResponse.sendError(404, "Payment not found");
                return;
            }

            // Security: shop owner can only download their own invoices; super admin can view any.
            // Re-checked here even when a scoped token was used.
            if (!isSuperAdmin && !payment.getShopOwnerId().equals(shopOwnerId)) {
                log.warn("Shop owner {} attempted to download invoice {} belonging to shop owner {}",
                        shopOwnerId, transactionUuid, payment.getShopOwnerId());
                httpServletResponse.sendError(403, "Access denied");
                return;
            }

            // Only successful payments have invoices
            if (payment.getStatus() != swari.sewa.module.payment.enums.PaymentStatus.SUCCESS) {
                httpServletResponse.sendError(400, "Invoice not available for this payment status");
                return;
            }

            // Audit: who looked at whose invoice.
            financeAuditService.recordInvoiceViewed(transactionUuid, payment.getInvoiceNumber(),
                    payment.getShopOwnerId(), isSuperAdmin ? "SUPERADMIN" : "SHOP_OWNER", shopOwnerId);

            // Get shop owner details (for super admin, use the payment's shop owner)
            Long invoiceShopOwnerId = isSuperAdmin ? payment.getShopOwnerId() : shopOwnerId;
            ShopOwner shopOwner = shopOwnerRepository.findById(invoiceShopOwnerId)
                    .orElseThrow(() -> new PaymentException("Shop owner not found"));

            // Plan name for the invoice, most-immutable source first.
            //
            // The Payment-level snapshot is the only field that is never
            // rewritten: the Subscription row is REUSED on renewal/upgrade and
            // its own snapshots are overwritten at that point, and the live plan
            // can be renamed or deleted outright. Preferring the subscription
            // snapshot (as this previously did) meant a renewal could silently
            // rewrite the plan name on an old invoice.
            String snapshotPlanName = payment.getPlanNameSnapshot();
            if (snapshotPlanName == null && payment.getSubscriptionId() != null) {
                var subOpt = subscriptionRepository.findById(payment.getSubscriptionId());
                if (subOpt.isPresent() && subOpt.get().getPlanNameSnapshot() != null) {
                    snapshotPlanName = subOpt.get().getPlanNameSnapshot();
                }
            }
            if (snapshotPlanName == null) {
                // Legacy payments predating the snapshot columns.
                snapshotPlanName = planService.getPlanEntity(payment.getSubscriptionPlanId()).getName();
            }

            // Get VAT settings for invoice label
            var vatSettings = settingsService.getSettingsEntity();
            String taxLabel = (Boolean.TRUE.equals(vatSettings.getEnableVat()) && vatSettings.getTaxPercentage() != null && vatSettings.getTaxPercentage() > 0)
                    ? "Tax / VAT (" + vatSettings.getTaxPercentage() + "%)"
                    : "Tax / VAT";

            // Format dates
            String paidDateStr = payment.getPaidAt() != null
                    ? payment.getPaidAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    : "—";
            String paidDateFullStr = payment.getPaidAt() != null
                    ? payment.getPaidAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                    : "—";

            // Billed period, preferring the immutable Payment-level snapshots over
            // the live Subscription row (which is overwritten on renewal).
            var dateFmt = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy");
            java.time.LocalDateTime periodStart = payment.getSubscriptionStartDateSnapshot();
            java.time.LocalDateTime periodEnd = payment.getSubscriptionEndDateSnapshot();
            if ((periodStart == null || periodEnd == null) && payment.getSubscriptionId() != null) {
                var subs = subscriptionRepository.findById(payment.getSubscriptionId());
                if (subs.isPresent()) {
                    var sub = subs.get();
                    if (periodStart == null) periodStart = sub.getStartDate();
                    if (periodEnd == null) periodEnd = sub.getEndDate();
                }
            }
            String subStartStr = periodStart != null ? periodStart.format(dateFmt) : "—";
            String subEndStr = periodEnd != null ? periodEnd.format(dateFmt) : "—";

            // Currency comes from the payment record, not a hardcoded symbol, so
            // a historical invoice always shows the currency it was settled in.
            String currency = payment.getCurrency() != null ? payment.getCurrency() : "NPR";

            // All shop-owner fields below are user-controlled and persisted, so
            // they must be HTML-escaped before interpolation. A Super Admin can
            // view any shop's invoice, which makes this a stored-XSS path.
            String rawShopName = shopOwner.getShopName() != null && !shopOwner.getShopName().isBlank()
                    ? shopOwner.getShopName()
                    : joinName(shopOwner.getFirstName(), shopOwner.getLastName());
            String shopName = HtmlEscape.escapeOr(rawShopName, "—");
            String ownerName = HtmlEscape.escapeOr(joinName(shopOwner.getFirstName(), shopOwner.getLastName()), "—");
            String shopEmail = HtmlEscape.escapeOr(shopOwner.getEmail(), "—");
            String shopPhone = HtmlEscape.escapeOr(shopOwner.getPhone(), "—");
            String shopAddress = HtmlEscape.escapeOr(shopOwner.getAddress(), "Nepal");
            String planNameSafe = HtmlEscape.escapeOr(snapshotPlanName, "—");
            String billingCycleSafe = HtmlEscape.escapeOr(payment.getBillingCycle(), "—");
            String gatewaySafe = HtmlEscape.escapeOr(payment.getGateway(), "—");
            String invoiceNumberSafe = HtmlEscape.escapeOr(payment.getInvoiceNumber(), "—");
            String transactionUuidSafe = HtmlEscape.escape(transactionUuid);
            String currencySafe = HtmlEscape.escape(currency);
            String taxLabelSafe = HtmlEscape.escape(taxLabel);

            String html = String.format("""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tax Invoice - %s</title>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap');
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', 'Segoe UI', Arial, sans-serif; background: #eef2f7; padding: 24px; color: #1e293b; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
        .invoice-wrap { max-width: 720px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 40px rgba(0,0,0,0.08); }
        /* ===== Top Banner ===== */
        .banner { background: linear-gradient(135deg, #1e293b 0%%, #0f172a 100%%); padding: 32px 40px; color: #fff; display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; flex-wrap: wrap; }
        .banner .brand-block { display: flex; align-items: flex-start; gap: 14px; }
        .banner .logo-badge { width: 52px; height: 52px; border-radius: 12px; background: linear-gradient(135deg, #f97316, #ea580c); display: flex; align-items: center; justify-content: center; font-size: 22px; font-weight: 800; color: #fff; flex-shrink: 0; }
        .banner .brand-info { line-height: 1.5; }
        .banner .brand-name { font-size: 20px; font-weight: 800; letter-spacing: 0.5px; }
        .banner .brand-tag { font-size: 11px; color: #94a3b8; margin-top: 1px; }
        .banner .brand-addr { font-size: 11px; color: #cbd5e1; margin-top: 8px; }
        .banner .brand-vat { font-size: 11px; color: #cbd5e1; margin-top: 2px; }
        .banner .brand-contact { font-size: 11px; color: #cbd5e1; margin-top: 2px; }
        .banner .brand-web { font-size: 11px; color: #cbd5e1; margin-top: 2px; }
        .banner .invoice-badge { text-align: right; flex-shrink: 0; }
        .banner .invoice-badge .tag { display: inline-block; padding: 5px 14px; border-radius: 20px; background: rgba(16,185,129,0.15); color: #34d399; font-size: 11px; font-weight: 700; letter-spacing: 1px; border: 1px solid rgba(16,185,129,0.3); }
        .banner .invoice-badge .inv-no { font-size: 15px; font-weight: 700; margin-top: 8px; color: #f8fafc; }
        .banner .invoice-badge .inv-date { font-size: 12px; color: #94a3b8; margin-top: 2px; }

        /* ===== Body ===== */
        .body { padding: 28px 40px; }
        .two-col { display: flex; gap: 20px; flex-wrap: wrap; margin-bottom: 24px; }
        .col { flex: 1; min-width: 240px; }
        .card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 16px 18px; height: 100%%; }
        .card .card-title { font-size: 10px; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase; color: #f97316; margin-bottom: 12px; display: flex; align-items: center; gap: 6px; }
        .card .card-title::before { content: ''; width: 14px; height: 2px; background: #f97316; border-radius: 2px; }
        .card .field { display: flex; margin: 5px 0; font-size: 13px; }
        .card .field .lbl { width: 90px; color: #94a3b8; font-weight: 500; flex-shrink: 0; }
        .card .field .val { color: #1e293b; font-weight: 600; }
        .card .product-line { font-size: 14px; font-weight: 700; color: #1e293b; margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px dashed #cbd5e1; }
        .card .product-line .picon { display: inline-block; width: 22px; height: 22px; border-radius: 6px; background: #fff7ed; color: #f97316; text-align: center; line-height: 22px; font-size: 12px; margin-right: 6px; }

        /* ===== Amount Table ===== */
        .amount-section { margin-bottom: 24px; }
        .amt-table { width: 100%%; border-collapse: separate; border-spacing: 0; border-radius: 10px; overflow: hidden; border: 1px solid #e2e8f0; }
        .amt-table thead th { background: #1e293b; color: #f1f5f9; font-size: 11px; font-weight: 600; letter-spacing: 0.5px; text-transform: uppercase; padding: 12px 18px; text-align: left; }
        .amt-table thead th:last-child { text-align: right; }
        .amt-table tbody td { padding: 12px 18px; font-size: 13px; border-bottom: 1px solid #f1f5f9; }
        .amt-table tbody td:last-child { text-align: right; font-weight: 600; }
        .amt-table tbody tr:last-child td { border-bottom: none; }
        .amt-table .total-row { background: linear-gradient(135deg, #fff7ed, #ffedd5); }
        .amt-table .total-row td { font-weight: 800; font-size: 15px; color: #1e293b; border-top: 2px solid #fdba74; }
        .amt-table .total-row td:last-child { color: #ea580c; font-size: 17px; }
        .amt-table .desc-cell strong { font-weight: 700; }
        .amt-table .desc-cell .sub { font-size: 11px; color: #94a3b8; margin-top: 2px; }

        /* ===== Payment Details ===== */
        .pay-section { background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 10px; padding: 16px 18px; margin-bottom: 24px; }
        .pay-section .pay-title { font-size: 10px; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase; color: #059669; margin-bottom: 12px; display: flex; align-items: center; gap: 6px; }
        .pay-section .pay-title::before { content: ''; width: 14px; height: 2px; background: #059669; border-radius: 2px; }
        .pay-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 20px; }
        .pay-grid .field { display: flex; font-size: 13px; }
        .pay-grid .field .lbl { width: 130px; color: #64748b; font-weight: 500; flex-shrink: 0; }
        .pay-grid .field .val { color: #1e293b; font-weight: 600; }
        .pay-grid .field .val.paid { color: #059669; }

        /* ===== Footer ===== */
        .footer { background: #1e293b; padding: 20px 40px; text-align: center; color: #94a3b8; }
        .footer .thanks { font-size: 14px; font-weight: 600; color: #f8fafc; margin-bottom: 4px; }
        .footer .product-line { font-size: 12px; margin-bottom: 8px; }
        .footer .copy { font-size: 10px; color: #64748b; }

        /* ===== Print Button ===== */
        .print-btn { position: fixed; top: 20px; right: 20px; padding: 12px 28px; border: none; border-radius: 10px; background: linear-gradient(135deg, #f97316, #ea580c); color: white; font-size: 14px; font-weight: 700; cursor: pointer; box-shadow: 0 4px 16px rgba(249,115,22,0.4); font-family: 'Inter', sans-serif; transition: transform 0.2s; }
        .print-btn:hover { transform: translateY(-2px); }
        @media print { .print-btn { display: none; } body { padding: 0; background: #fff; } .invoice-wrap { box-shadow: none; border-radius: 0; max-width: 100%%; } }
        @media (max-width: 600px) { .banner { padding: 24px 20px; } .body { padding: 20px; } .company-bar { padding: 12px 20px; } .footer { padding: 16px 20px; } .pay-grid { grid-template-columns: 1fr; } }
    </style>
</head>
<body>
    <button class="print-btn" onclick="window.print()">Print / Save PDF</button>
    <div class="invoice-wrap">
        <!-- ===== Top Banner ===== -->
        <div class="banner">
            <div class="brand-block">
                <div class="logo-badge">M</div>
                <div class="brand-info">
                    <div class="brand-name">MYCON DIGITAL</div>
                    <div class="brand-tag">Software &amp; Technology Solutions</div>
                    <div class="brand-addr">Janakpur-09, Dhanusha, Madhesh, Nepal</div>
                    <div class="brand-vat">PAN/VAT No: 1023042404</div>
                    <div class="brand-contact">Email: support@mycondigital.com &nbsp;|&nbsp; Phone: 9804896396</div>
                    <div class="brand-web">Website: www.mycondigital.com</div>
                </div>
            </div>
            <div class="invoice-badge">
                <div class="tag">PAID</div>
                <div class="inv-no">%s</div>
                <div class="inv-date">%s</div>
            </div>
        </div>

        <!-- ===== Body ===== -->
        <div class="body">
            <div style="font-size: 11px; font-weight: 700; letter-spacing: 2px; text-transform: uppercase; color: #94a3b8; margin-bottom: 16px;">Tax Invoice</div>

            <!-- Billed To + Product/Service -->
            <div class="two-col">
                <div class="col">
                    <div class="card">
                        <div class="card-title">Billed To</div>
                        <div class="field"><span class="lbl">Shop Name</span><span class="val">%s</span></div>
                        <div class="field"><span class="lbl">Owner</span><span class="val">%s</span></div>
                        <div class="field"><span class="lbl">Address</span><span class="val">%s</span></div>
                        <div class="field"><span class="lbl">Email</span><span class="val">%s</span></div>
                        <div class="field"><span class="lbl">Phone</span><span class="val">%s</span></div>
                    </div>
                </div>
                <div class="col">
                    <div class="card">
                        <div class="card-title">Product / Service</div>
                        <div class="product-line"><span class="picon">S</span>Swari Sadhan &mdash; Business Management Software</div>
                        <div class="field"><span class="lbl">Plan</span><span class="val">%s Plan</span></div>
                        <div class="field"><span class="lbl">Billing Cycle</span><span class="val" style="text-transform: capitalize;">%s</span></div>
                        <div class="field"><span class="lbl">Period</span><span class="val">%s &ndash; %s</span></div>
                    </div>
                </div>
            </div>

            <!-- Amount Table -->
            <div class="amount-section">
                <table class="amt-table">
                    <thead>
                        <tr>
                            <th>Description</th>
                            <th>Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td class="desc-cell"><strong>Swari Sadhan %s Plan</strong><div class="sub">Subscription &middot; %s billing cycle</div></td>
                            <td>%s %s</td>
                        </tr>
                        %s
                        <tr>
                            <td class="desc-cell"><strong>%s</strong></td>
                            <td>%s %s</td>
                        </tr>
                        <tr class="total-row">
                            <td>TOTAL</td>
                            <td>%s %s</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- Payment Details -->
            <div class="pay-section">
                <div class="pay-title">Payment Details</div>
                <div class="pay-grid">
                    <div class="field"><span class="lbl">Gateway</span><span class="val">%s</span></div>
                    <div class="field"><span class="lbl">Transaction ID</span><span class="val" style="font-family: monospace; font-size: 12px;">%s</span></div>
                    <div class="field"><span class="lbl">Payment Date</span><span class="val">%s</span></div>
                    <div class="field"><span class="lbl">Status</span><span class="val paid">PAID</span></div>
                </div>
            </div>
        </div>

        <!-- ===== Footer ===== -->
        <div class="footer">
            <div class="thanks">Thank you for choosing MYCON Digital.</div>
            <div class="product-line">Swari Sadhan is a product of MYCON Digital.</div>
            <div class="copy">This is a computer-generated invoice and does not require a signature. &copy; 2026 MYCON Digital.</div>
        </div>
    </div>
</body>
</html>
                """,
                invoiceNumberSafe,                       // title
                invoiceNumberSafe,                       // invoice no (banner)
                paidDateStr,                             // invoice date (banner)
                shopName,                                // shop name
                ownerName,                               // owner
                shopAddress,                             // address
                shopEmail,                               // email
                shopPhone,                               // phone
                planNameSafe,                            // plan name
                billingCycleSafe,                        // billing cycle
                subStartStr,                             // subscription start
                subEndStr,                               // subscription end
                planNameSafe,                            // plan name (table)
                billingCycleSafe,                        // billing cycle (table sub)
                currencySafe,                            // subtotal currency
                plainAmount(payment.getAmount()),        // amount
                discountRow(payment, currencySafe),      // discount row (empty if no discount)
                taxLabelSafe,                            // tax label
                currencySafe,                            // tax currency
                plainAmount(payment.getTaxAmount()),     // tax
                currencySafe,                            // total currency
                plainAmount(payment.getTotalAmount()),   // total
                gatewaySafe,                             // payment gateway
                transactionUuidSafe,                     // transaction ID
                paidDateFullStr                          // payment date
            );

            httpServletResponse.setContentType("text/html;charset=UTF-8");
            httpServletResponse.setHeader("Content-Disposition",
                    "inline; filename=invoice_" + sanitizeFilename(payment.getInvoiceNumber()) + ".html");
            // Financial documents must not be cached by browsers or shared proxies.
            httpServletResponse.setHeader("Cache-Control", "no-store, private");
            httpServletResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpServletResponse.getWriter().write(html);
        } catch (Exception e) {
            // Do not surface exception details to the client — they can reveal
            // internals. The full stack trace stays in the server log.
            log.error("Invoice render failed for transaction_uuid={}: {}", transactionUuid, e.getMessage(), e);
            httpServletResponse.sendError(500, "Failed to generate invoice");
        }
    }

    // ===== Helpers =====

    private static String joinName(String first, String last) {
        String f = first != null ? first : "";
        String l = last != null ? last : "";
        String joined = (f + " " + l).trim();
        return joined.isEmpty() ? null : joined;
    }

    private static String plainAmount(java.math.BigDecimal value) {
        return value != null ? value.toPlainString() : "0.00";
    }

    /** Optional coupon-discount line. Coupon codes are user-supplied, so escaped. */
    private static String discountRow(swari.sewa.module.payment.entity.Payment payment, String currencySafe) {
        var discount = payment.getDiscountAmount();
        if (discount == null || discount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return "";
        }
        String code = payment.getCouponCodeSnapshot();
        String label = code != null && !code.isBlank()
                ? "Coupon Discount (" + HtmlEscape.escape(code) + ")"
                : "Coupon Discount";
        return "<tr><td class=\"desc-cell\"><strong>" + label
                + "</strong></td><td style=\"color:#059669;\">- " + currencySafe + " "
                + discount.toPlainString() + "</td></tr>";
    }

    /** Strip anything that could break out of the Content-Disposition header. */
    private static String sanitizeFilename(String value) {
        if (value == null || value.isBlank()) {
            return "invoice";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private Long resolveShopOwnerIdFromToken(String token) {
        if (token == null || !jwtUtil.validateToken(token)) {
            return null;
        }
        String email = jwtUtil.getEmailFromToken(token);
        return shopOwnerRepository.findByEmail(email)
                .map(ShopOwner::getId)
                .orElse(null);
    }

    private boolean isCurrentUserSuperAdmin(String token) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            for (var a : auth.getAuthorities()) {
                if ("ROLE_SUPERADMIN".equals(a.getAuthority())) return true;
            }
        }
        if (token != null && !token.isEmpty() && jwtUtil.validateToken(token)) {
            String role = jwtUtil.getRoleFromToken(token);
            return "SUPERADMIN".equalsIgnoreCase(role);
        }
        return false;
    }
}
