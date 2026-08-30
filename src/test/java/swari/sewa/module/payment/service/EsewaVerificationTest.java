package swari.sewa.module.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import swari.sewa.module.payment.config.EsewaConfig;
import swari.sewa.module.payment.dto.PaymentResponse;
import swari.sewa.module.payment.entity.Payment;
import swari.sewa.module.payment.enums.PaymentStatus;
import swari.sewa.module.payment.exception.PaymentException;
import swari.sewa.module.payment.repository.PaymentRepository;
import swari.sewa.module.payment.service.impl.EsewaPaymentServiceImpl;
import swari.sewa.module.subscription.entity.Subscription;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.entity.SubscriptionPlanPricing;
import swari.sewa.module.subscription.entity.SubscriptionPlanRestriction;
import swari.sewa.module.subscription.entity.SubscriptionSettings;
import swari.sewa.module.subscription.enums.PlanStatus;
import swari.sewa.module.subscription.enums.SubscriptionStatus;
import swari.sewa.module.subscription.repository.SubscriptionCouponUsageRepository;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.entity.SubscriptionTransaction;
import swari.sewa.module.subscription.repository.SubscriptionTransactionRepository;
import swari.sewa.module.subscription.service.InvoiceService;
import swari.sewa.module.subscription.service.SubscriptionCouponService;
import swari.sewa.module.subscription.service.SubscriptionPlanService;
import swari.sewa.module.subscription.service.SubscriptionSettingsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for eSewa server-side payment verification.
 *
 * Uses a test subclass that overrides verifyWithEsewa() to simulate
 * eSewa's status API responses without making real HTTP calls.
 *
 * Business rules verified:
 * - COMPLETE → payment SUCCESS, subscription ACTIVE, invoice generated, transaction created
 * - PENDING → payment remains PENDING, no side effects
 * - CANCELLED → payment CANCELLED, no activation
 * - NOT_FOUND → payment CANCELLED, no activation
 * - AMBIGUOUS → payment VERIFICATION_FAILED, no activation
 * - FULL_REFUND → payment FAILED, no activation
 * - Wrong amount → VERIFICATION_FAILED
 * - Wrong product code → VERIFICATION_FAILED
 * - Missing status → VERIFICATION_FAILED
 * - Already SUCCESS → idempotent, no reprocessing
 * - Already FAILED → success callback ignored
 * - Already CANCELLED → success callback ignored
 * - Duplicate callback → no duplicate side effects
 * - Coupon usage recorded only on SUCCESS
 * - Expense synchronized only on SUCCESS
 */
class EsewaVerificationTest {

    private TestableEsewaPaymentService service;
    private PaymentRepository paymentRepository;
    private EsewaConfig esewaConfig;
    private EsewaSignatureService signatureService;
    private SubscriptionPlanService planService;
    private SubscriptionRepository subscriptionRepository;
    private SubscriptionTransactionRepository transactionRepository;
    private InvoiceService invoiceService;
    private PaymentEmailService paymentEmailService;
    private PaymentExpenseSyncService paymentExpenseSyncService;
    private SubscriptionSettingsService settingsService;
    private SubscriptionCouponService couponService;
    private SubscriptionCouponUsageRepository couponUsageRepository;
    private CouponUsageRecorder couponUsageRecorder;
    private swari.sewa.module.vehicle.repository.VehicleRepository vehicleRepository;

    private static final String TXN_UUID = "SS-20260822-123456";
    private static final String PRODUCT_CODE = "EPAYTEST";
    private static final BigDecimal PLAN_PRICE = new BigDecimal("5399.00");
    private static final BigDecimal TAX_AMOUNT = new BigDecimal("701.87");
    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("6100.87");

    @BeforeEach
    void setUp() {
        paymentRepository = Mockito.mock(PaymentRepository.class);
        esewaConfig = Mockito.mock(EsewaConfig.class);
        signatureService = Mockito.mock(EsewaSignatureService.class);
        planService = Mockito.mock(SubscriptionPlanService.class);
        subscriptionRepository = Mockito.mock(SubscriptionRepository.class);
        transactionRepository = Mockito.mock(SubscriptionTransactionRepository.class);
        invoiceService = Mockito.mock(InvoiceService.class);
        paymentEmailService = Mockito.mock(PaymentEmailService.class);
        paymentExpenseSyncService = Mockito.mock(PaymentExpenseSyncService.class);
        settingsService = Mockito.mock(SubscriptionSettingsService.class);
        couponService = Mockito.mock(SubscriptionCouponService.class);
        couponUsageRepository = Mockito.mock(SubscriptionCouponUsageRepository.class);
        couponUsageRecorder = Mockito.mock(CouponUsageRecorder.class);

        vehicleRepository = Mockito.mock(swari.sewa.module.vehicle.repository.VehicleRepository.class);

        service = new TestableEsewaPaymentService(
                paymentRepository, esewaConfig, signatureService, planService,
                subscriptionRepository, transactionRepository, invoiceService,
                paymentEmailService, paymentExpenseSyncService, settingsService,
                couponService, couponUsageRepository, couponUsageRecorder, vehicleRepository);

        when(esewaConfig.getProductCode()).thenReturn(PRODUCT_CODE);
        when(esewaConfig.getSecretKey()).thenReturn("8gBm/:&EnhH.1/q");
        when(esewaConfig.getStatusUrl()).thenReturn("https://rc.esewa.com.np/api/epay/transaction/status/");

        // Default plan for activation
        SubscriptionPlan plan = createPublishedPlan(7L, "Business", 10);
        when(planService.getPlanEntity(7L)).thenReturn(plan);

        // Default settings
        when(settingsService.getSettingsEntity()).thenReturn(
                SubscriptionSettings.builder().id(1L).enableVat(true).taxPercentage(13).build());

        // Invoice generation
        when(invoiceService.generateInvoiceNumber()).thenReturn("INV-2026-00001");

        // Subscription save returns subscription with ID
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        // Transaction save returns transaction with ID
        when(transactionRepository.save(any(SubscriptionTransaction.class))).thenAnswer(inv -> {
            SubscriptionTransaction t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        // Idempotency check: no existing transaction for a fresh callback
        when(transactionRepository.findByTransactionId(any(String.class)))
                .thenReturn(java.util.Optional.empty());

        // Payment save returns payment with ID
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });
    }

    private SubscriptionPlan createPublishedPlan(Long id, String name, Integer maxVehicles) {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .id(id).name(name).slug(name.toLowerCase()).status(PlanStatus.PUBLISHED).build();
        SubscriptionPlanPricing pricing = SubscriptionPlanPricing.builder()
                .plan(plan).monthly(new BigDecimal("599")).yearly(PLAN_PRICE).currency("NPR").build();
        Set<SubscriptionPlanPricing> pricings = new HashSet<>();
        pricings.add(pricing);
        plan.setPricings(pricings);
        if (maxVehicles != null) {
            SubscriptionPlanRestriction restriction = SubscriptionPlanRestriction.builder()
                    .plan(plan).maxVehicles(maxVehicles).build();
            Set<SubscriptionPlanRestriction> restrictions = new HashSet<>();
            restrictions.add(restriction);
            plan.setRestrictions(restrictions);
        }
        return plan;
    }

    private Payment createPendingPayment(Long couponId) {
        return Payment.builder()
                .id(1L)
                .transactionUuid(TXN_UUID)
                .gateway("ESEWA")
                .shopOwnerId(100L)
                .subscriptionPlanId(7L)
                .billingCycle("yearly")
                .amount(PLAN_PRICE)
                .taxAmount(TAX_AMOUNT)
                .totalAmount(TOTAL_AMOUNT)
                .currency("NPR")
                .status(PaymentStatus.PENDING)
                .productCode(PRODUCT_CODE)
                .couponId(couponId)
                .discountAmount(couponId != null ? new BigDecimal("500.00") : BigDecimal.ZERO)
                .build();
    }

    // ===== COMPLETE → SUCCESS =====

    @Nested
    @DisplayName("COMPLETE status → full success flow")
    class CompleteStatusTests {

        @Test
        @DisplayName("COMPLETE → payment SUCCESS, subscription ACTIVE, invoice generated, transaction created")
        void testComplete_fullSuccessFlow() {
            Payment payment = createPendingPayment(null);
            when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

            service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                    "COMPLETE", "REF123", "6101", PRODUCT_CODE));

            PaymentResponse response = service.handleSuccessCallback(TXN_UUID, "6101", "TXN123", "REF456", "SUCCESS");

            assertEquals("SUCCESS", response.getStatus());
            assertEquals("TXN123", response.getGatewayTransactionId());
            assertEquals("REF456", response.getGatewayRefId());
            assertNotNull(response.getPaidAt());

            // Verify invoice was generated
            verify(invoiceService).generateInvoiceNumber();

            // Verify subscription was activated
            verify(subscriptionRepository).save(argThat(s ->
                    s.getStatus() == SubscriptionStatus.ACTIVE &&
                    s.getShopOwnerId().equals(100L) &&
                    s.getPlanNameSnapshot().equals("Business") &&
                    s.getVehicleLimitSnapshot() == 120 // 10 monthly × 12 yearly
            ));

            // Verify transaction record was created
            verify(transactionRepository).save(any());

            // Verify expense was synced
            verify(paymentExpenseSyncService).createSubscriptionExpense(any(), any());

            // Verify email was sent
            verify(paymentEmailService).sendPaymentSuccessEmail(any(), any());
        }

        @Test
        @DisplayName("COMPLETE with coupon → coupon usage recorded")
        void testCompleteWithCoupon_couponUsageRecorded() {
            Payment payment = createPendingPayment(1L);
            // With coupon: total = (5399 - 500) + tax on 4899 = 4899 + 636.87 = 5535.87 → 5536
            payment.setTotalAmount(new BigDecimal("5535.87"));
            when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

            service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                    "COMPLETE", "REF123", "5536", PRODUCT_CODE));

            service.handleSuccessCallback(TXN_UUID, "5536", "TXN123", "REF456", "SUCCESS");

            verify(couponUsageRecorder).recordUsage(eq(1L), eq(1L), eq(100L), eq(new BigDecimal("500.00")));
        }

        @Test
        @DisplayName("COMPLETE → subscription snapshot stores vehicle limit = monthly × cycle months")
        void testComplete_vehicleLimitSnapshot_isMonthlyTimesCycle() {
            Payment payment = createPendingPayment(null);
            when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

            service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                    "COMPLETE", "REF123", "6101", PRODUCT_CODE));

            service.handleSuccessCallback(TXN_UUID, "6101", "TXN123", "REF456", "SUCCESS");

            // Plan has maxVehicles=10, billing cycle=yearly → 10 × 12 = 120
            verify(subscriptionRepository).save(argThat(s ->
                    s.getVehicleLimitSnapshot() == 120 &&
                    s.getBillingCycleSnapshot().equals("yearly") &&
                    s.getPricePaid().compareTo(PLAN_PRICE) == 0
            ));
        }
    }

    // ===== PENDING → no activation =====

    @Test
    @DisplayName("PENDING → payment remains PENDING, no side effects")
    void testPending_noActivation() {
        Payment payment = createPendingPayment(null);
        when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

        service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                "PENDING", null, "6101", PRODUCT_CODE));

        PaymentResponse response = service.handleSuccessCallback(TXN_UUID, "6101", null, null, "PENDING");

        assertEquals("PENDING", response.getStatus());
        verify(invoiceService, never()).generateInvoiceNumber();
        verify(subscriptionRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(paymentExpenseSyncService, never()).createSubscriptionExpense(any(), any());
        verify(paymentEmailService, never()).sendPaymentSuccessEmail(any(), any());
    }

    // ===== CANCELLED / NOT_FOUND =====

    @Test
    @DisplayName("CANCELED → payment CANCELLED, no activation")
    void testCancelled_noActivation() {
        Payment payment = createPendingPayment(null);
        when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

        // eSewa uses American spelling "CANCELED" and amount must match for verification to pass
        service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                "CANCELED", null, "6101", PRODUCT_CODE));

        PaymentResponse response = service.handleSuccessCallback(TXN_UUID, "6101", null, null, "CANCELED");

        assertEquals("CANCELLED", response.getStatus());
        verify(subscriptionRepository, never()).save(any());
        verify(paymentExpenseSyncService, never()).createSubscriptionExpense(any(), any());
    }

    @Test
    @DisplayName("NOT_FOUND → payment CANCELLED, no activation")
    void testNotFound_noActivation() {
        Payment payment = createPendingPayment(null);
        when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

        // Amount must match for verification to reach the status switch
        service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                "NOT_FOUND", null, "6101", PRODUCT_CODE));

        PaymentResponse response = service.handleSuccessCallback(TXN_UUID, "6101", null, null, "NOT_FOUND");

        assertEquals("CANCELLED", response.getStatus());
        verify(subscriptionRepository, never()).save(any());
    }

    // ===== AMBIGUOUS → VERIFICATION_FAILED =====

    @Test
    @DisplayName("AMBIGUOUS → payment VERIFICATION_FAILED, no activation")
    void testAmbiguous_verificationFailed() {
        Payment payment = createPendingPayment(null);
        when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

        service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                "AMBIGUOUS", null, "6101", PRODUCT_CODE));

        PaymentResponse response = service.handleSuccessCallback(TXN_UUID, "6101", null, null, "AMBIGUOUS");

        assertEquals("VERIFICATION_FAILED", response.getStatus());
        verify(subscriptionRepository, never()).save(any());
    }

    // ===== FULL_REFUND → FAILED =====

    @Test
    @DisplayName("FULL_REFUND → payment FAILED, no activation")
    void testFullRefund_paymentFailed() {
        Payment payment = createPendingPayment(null);
        when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

        service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                "FULL_REFUND", null, "6101", PRODUCT_CODE));

        PaymentResponse response = service.handleSuccessCallback(TXN_UUID, "6101", null, null, "FULL_REFUND");

        assertEquals("FAILED", response.getStatus());
        verify(subscriptionRepository, never()).save(any());
    }

    // ===== Wrong Amount =====

    @Test
    @DisplayName("Wrong amount → VERIFICATION_FAILED, no activation")
    void testWrongAmount_verificationFailed() {
        Payment payment = createPendingPayment(null);
        when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

        // eSewa returns amount 500 but payment total is 6100.87
        service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                "COMPLETE", "REF123", "500", PRODUCT_CODE));

        PaymentResponse response = service.handleSuccessCallback(TXN_UUID, "500", "TXN123", "REF456", "SUCCESS");

        assertEquals("VERIFICATION_FAILED", response.getStatus());
        assertNotNull(response.getFailureReason());
        assertTrue(response.getFailureReason().contains("Amount mismatch"));
        verify(subscriptionRepository, never()).save(any());
    }

    // ===== Wrong Product Code =====

    @Test
    @DisplayName("Wrong product code → VERIFICATION_FAILED, no activation")
    void testWrongProductCode_verificationFailed() {
        Payment payment = createPendingPayment(null);
        when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

        service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                "COMPLETE", "REF123", "6101", "WRONG_PRODUCT"));

        PaymentResponse response = service.handleSuccessCallback(TXN_UUID, "6101", "TXN123", "REF456", "SUCCESS");

        assertEquals("VERIFICATION_FAILED", response.getStatus());
        assertTrue(response.getFailureReason().contains("Product code mismatch"));
        verify(subscriptionRepository, never()).save(any());
    }

    // ===== Missing Status =====

    @Test
    @DisplayName("Missing status → VERIFICATION_FAILED, no activation")
    void testMissingStatus_verificationFailed() {
        Payment payment = createPendingPayment(null);
        when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

        service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                null, "REF123", "6101", PRODUCT_CODE));

        PaymentResponse response = service.handleSuccessCallback(TXN_UUID, "6101", "TXN123", "REF456", "SUCCESS");

        assertEquals("VERIFICATION_FAILED", response.getStatus());
        verify(subscriptionRepository, never()).save(any());
    }

    // ===== Idempotency =====

    @Nested
    @DisplayName("Idempotency — duplicate callbacks")
    class IdempotencyTests {

        @Test
        @DisplayName("Already SUCCESS → no reprocessing")
        void testAlreadySuccess_noReprocessing() {
            Payment payment = createPendingPayment(null);
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setInvoiceNumber("INV-2026-00001");
            payment.setSubscriptionId(1L);
            when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

            service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                    "COMPLETE", "REF123", "6101", PRODUCT_CODE));

            PaymentResponse response = service.handleSuccessCallback(TXN_UUID, "6101", "TXN123", "REF456", "SUCCESS");

            assertEquals("SUCCESS", response.getStatus());
            verify(invoiceService, never()).generateInvoiceNumber();
            verify(subscriptionRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
            verify(paymentExpenseSyncService, never()).createSubscriptionExpense(any(), any());
            verify(couponUsageRecorder, never()).recordUsage(anyLong(), anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("Already FAILED → success callback ignored")
        void testAlreadyFailed_ignored() {
            Payment payment = createPendingPayment(null);
            payment.setStatus(PaymentStatus.FAILED);
            when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

            service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                    "COMPLETE", "REF123", "6101", PRODUCT_CODE));

            PaymentResponse response = service.handleSuccessCallback(TXN_UUID, "6101", "TXN123", "REF456", "SUCCESS");

            assertEquals("FAILED", response.getStatus());
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Already CANCELLED → success callback ignored")
        void testAlreadyCancelled_ignored() {
            Payment payment = createPendingPayment(null);
            payment.setStatus(PaymentStatus.CANCELLED);
            when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

            service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                    "COMPLETE", "REF123", "6101", PRODUCT_CODE));

            PaymentResponse response = service.handleSuccessCallback(TXN_UUID, "6101", "TXN123", "REF456", "SUCCESS");

            assertEquals("CANCELLED", response.getStatus());
            verify(subscriptionRepository, never()).save(any());
        }
    }

    // ===== Payment Not Found =====

    @Test
    @DisplayName("Payment not found → PaymentException")
    void testPaymentNotFound_throws() {
        when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.empty());

        assertThrows(PaymentException.class,
                () -> service.handleSuccessCallback(TXN_UUID, "6101", "TXN123", "REF456", "SUCCESS"));
    }

    // ===== Failure Callback =====

    @Test
    @DisplayName("Failure callback → payment FAILED")
    void testFailureCallback_paymentFailed() {
        Payment payment = createPendingPayment(null);
        when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

        PaymentResponse response = service.handleFailureCallback(TXN_UUID, "User cancelled payment");

        assertEquals("FAILED", response.getStatus());
        assertEquals("User cancelled payment", response.getFailureReason());
    }

    @Test
    @DisplayName("Failure callback on already SUCCESS → ignored")
    void testFailureCallbackOnSuccess_ignored() {
        Payment payment = createPendingPayment(null);
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

        PaymentResponse response = service.handleFailureCallback(TXN_UUID, "Late failure callback");

        assertEquals("SUCCESS", response.getStatus());
    }

    // ===== Coupon Not Recorded on Non-Success =====

    @Test
    @DisplayName("CANCELLED → coupon usage NOT recorded")
    void testCancelled_noCouponUsage() {
        Payment payment = createPendingPayment(1L);
        when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

        service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                "CANCELLED", null, "6101", PRODUCT_CODE));

        service.handleSuccessCallback(TXN_UUID, "6101", null, null, "CANCELLED");

        verify(couponUsageRecorder, never()).recordUsage(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("Wrong amount → coupon usage NOT recorded")
    void testWrongAmount_noCouponUsage() {
        Payment payment = createPendingPayment(1L);
        when(paymentRepository.findByTransactionUuid(TXN_UUID)).thenReturn(Optional.of(payment));

        service.setEsewaStatusResult(new EsewaPaymentServiceImpl.EsewaStatusResult(
                "COMPLETE", "REF123", "500", PRODUCT_CODE));

        service.handleSuccessCallback(TXN_UUID, "500", "TXN123", "REF456", "SUCCESS");

        verify(couponUsageRecorder, never()).recordUsage(anyLong(), anyLong(), anyLong(), any());
    }

    // ===== Test Subclass =====

    /**
     * Test subclass that overrides verifyWithEsewa to avoid real HTTP calls.
     * The parent class's verifyWithEsewa is protected for this exact purpose.
     */
    private static class TestableEsewaPaymentService extends EsewaPaymentServiceImpl {
        private EsewaStatusResult mockResult;

        TestableEsewaPaymentService(
                PaymentRepository paymentRepository, EsewaConfig esewaConfig,
                EsewaSignatureService signatureService, SubscriptionPlanService planService,
                SubscriptionRepository subscriptionRepository,
                SubscriptionTransactionRepository transactionRepository,
                InvoiceService invoiceService, PaymentEmailService paymentEmailService,
                PaymentExpenseSyncService paymentExpenseSyncService,
                SubscriptionSettingsService settingsService,
                SubscriptionCouponService couponService,
                SubscriptionCouponUsageRepository couponUsageRepository,
                CouponUsageRecorder couponUsageRecorder,
                swari.sewa.module.vehicle.repository.VehicleRepository vehicleRepository) {
            super(paymentRepository, esewaConfig, signatureService, planService,
                    subscriptionRepository, transactionRepository, invoiceService,
                    paymentEmailService, paymentExpenseSyncService, settingsService,
                    couponService, couponUsageRepository, couponUsageRecorder, vehicleRepository);
        }

        void setEsewaStatusResult(EsewaStatusResult result) {
            this.mockResult = result;
        }

        @Override
        protected EsewaStatusResult verifyWithEsewa(Payment payment) {
            if (mockResult != null) return mockResult;
            throw new IllegalStateException("Test must set mock eSewa status result");
        }
    }
}
