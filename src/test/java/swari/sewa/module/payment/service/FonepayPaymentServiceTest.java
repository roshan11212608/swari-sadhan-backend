package swari.sewa.module.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import swari.sewa.module.payment.config.FonepayConfig;
import swari.sewa.module.payment.dto.CreatePaymentRequest;
import swari.sewa.module.payment.dto.PaymentResponse;
import swari.sewa.module.payment.entity.Payment;
import swari.sewa.module.payment.enums.PaymentStatus;
import swari.sewa.module.payment.exception.PaymentException;
import swari.sewa.module.payment.repository.PaymentRepository;
import swari.sewa.module.payment.service.impl.FonepayPaymentServiceImpl;
import swari.sewa.module.subscription.entity.Subscription;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.entity.SubscriptionPlanPricing;
import swari.sewa.module.subscription.entity.SubscriptionPlanRestriction;
import swari.sewa.module.subscription.entity.SubscriptionSettings;
import swari.sewa.module.subscription.entity.SubscriptionTransaction;
import swari.sewa.module.subscription.enums.PlanStatus;
import swari.sewa.module.subscription.enums.SubscriptionStatus;
import swari.sewa.module.subscription.repository.SubscriptionRepository;
import swari.sewa.module.subscription.repository.SubscriptionTransactionRepository;
import swari.sewa.module.subscription.service.InvoiceService;
import swari.sewa.module.subscription.service.SubscriptionPlanService;
import swari.sewa.module.subscription.service.SubscriptionSettingsService;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FonepayPaymentServiceImpl — Fonepay payment gateway integration.
 *
 * Business rules verified:
 * - Payment creation generates correct signed URL
 * - Callback signature verification (mocked)
 * - Successful callback → payment SUCCESS, subscription ACTIVE
 * - Failed callback → payment FAILED
 * - Invalid signature → VERIFICATION_FAILED
 * - Wrong merchant code → VERIFICATION_FAILED
 * - Wrong amount → VERIFICATION_FAILED
 * - Non-success status → payment FAILED
 * - Idempotency: already SUCCESS → no reprocessing
 * - Duplicate callback → no duplicate side effects
 */
class FonepayPaymentServiceTest {

    private FonepayPaymentServiceImpl service;
    private PaymentRepository paymentRepository;
    private FonepayConfig fonepayConfig;
    private FonepaySignatureService signatureService;
    private SubscriptionPlanService planService;
    private SubscriptionRepository subscriptionRepository;
    private SubscriptionTransactionRepository transactionRepository;
    private InvoiceService invoiceService;
    private PaymentEmailService paymentEmailService;
    private PaymentExpenseSyncService paymentExpenseSyncService;
    private SubscriptionSettingsService settingsService;

    private static final String PRN = "SS-20260822-123456";
    private static final String PID = "TEST_MERCHANT";
    private static final String SECRET = "test-secret";
    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("6100.87");

    @BeforeEach
    void setUp() {
        paymentRepository = Mockito.mock(PaymentRepository.class);
        fonepayConfig = Mockito.mock(FonepayConfig.class);
        signatureService = Mockito.mock(FonepaySignatureService.class);
        planService = Mockito.mock(SubscriptionPlanService.class);
        subscriptionRepository = Mockito.mock(SubscriptionRepository.class);
        transactionRepository = Mockito.mock(SubscriptionTransactionRepository.class);
        invoiceService = Mockito.mock(InvoiceService.class);
        paymentEmailService = Mockito.mock(PaymentEmailService.class);
        paymentExpenseSyncService = Mockito.mock(PaymentExpenseSyncService.class);
        settingsService = Mockito.mock(SubscriptionSettingsService.class);

        service = new FonepayPaymentServiceImpl(
                paymentRepository, fonepayConfig, signatureService, planService,
                subscriptionRepository, transactionRepository, invoiceService,
                paymentEmailService, paymentExpenseSyncService, settingsService, mock(swari.sewa.module.vehicle.repository.VehicleRepository.class));

        when(fonepayConfig.getMerchantCodePid()).thenReturn(PID);
        when(fonepayConfig.getMerchantSecretKey()).thenReturn(SECRET);
        when(fonepayConfig.getPaymentUrl()).thenReturn("https://dev.fonepay.com/api/qrRequest");
        when(fonepayConfig.getBackendReturnUrl()).thenReturn("http://localhost:8081/api/payments/fonepay/verify");

        when(signatureService.generateSignature(anyString(), anyString())).thenReturn("mock-signature");

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });
        when(paymentRepository.existsByTransactionUuid(anyString())).thenReturn(false);

        // Default plan for activation
        SubscriptionPlan plan = createPublishedPlan(7L, "Business", 10);
        when(planService.getPlanEntity(7L)).thenReturn(plan);

        when(settingsService.getSettingsEntity()).thenReturn(
                SubscriptionSettings.builder().id(1L).enableVat(true).taxPercentage(13).build());

        when(invoiceService.generateInvoiceNumber()).thenReturn("INV-2026-00001");

        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        // Idempotency check: no existing transaction for a fresh callback
        when(transactionRepository.findByTransactionId(any(String.class)))
                .thenReturn(java.util.Optional.empty());
        when(transactionRepository.save(any(SubscriptionTransaction.class))).thenAnswer(inv -> {
            SubscriptionTransaction t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
    }

    private SubscriptionPlan createPublishedPlan(Long id, String name, Integer maxVehicles) {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .id(id).name(name).slug(name.toLowerCase()).status(PlanStatus.PUBLISHED).build();
        SubscriptionPlanPricing pricing = SubscriptionPlanPricing.builder()
                .plan(plan).monthly(new BigDecimal("599")).yearly(new BigDecimal("5399")).currency("NPR").build();
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

    private Payment createPendingPayment() {
        return Payment.builder()
                .id(1L)
                .transactionUuid(PRN)
                .gateway("FONEPAY")
                .shopOwnerId(100L)
                .subscriptionPlanId(7L)
                .billingCycle("yearly")
                .amount(new BigDecimal("5399.00"))
                .taxAmount(new BigDecimal("701.87"))
                .totalAmount(TOTAL_AMOUNT)
                .currency("NPR")
                .status(PaymentStatus.PENDING)
                .build();
    }

    private CreatePaymentRequest createRequest() {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setPlanId(7L);
        req.setBillingCycle("yearly");
        return req;
    }

    // ===== Payment Creation =====

    @Nested
    @DisplayName("Payment creation")
    class PaymentCreationTests {

        @Test
        @DisplayName("Valid payment creation → returns signed URL")
        void testValidPaymentCreation() {
            SubscriptionPlan plan = createPublishedPlan(7L, "Business", 10);
            when(planService.getPlanEntity(7L)).thenReturn(plan);
            when(settingsService.getSettingsEntity()).thenReturn(
                    SubscriptionSettings.builder().id(1L).enableVat(true).taxPercentage(13).build());

            String result = service.createPayment(createRequest(), 100L);

            assertNotNull(result);
            assertTrue(result.contains("qrRequest") || result.contains("fonepay"));
            verify(paymentRepository).save(argThat(p ->
                    p.getStatus() == PaymentStatus.PENDING &&
                    p.getGateway().equals("FONEPAY") &&
                    p.getShopOwnerId().equals(100L)
            ));
        }

        @Test
        @DisplayName("Unpublished plan → PaymentException")
        void testUnpublishedPlan_throws() {
            SubscriptionPlan plan = createPublishedPlan(7L, "Business", 10);
            plan.setStatus(PlanStatus.DRAFT);
            when(planService.getPlanEntity(7L)).thenReturn(plan);

            assertThrows(PaymentException.class,
                    () -> service.createPayment(createRequest(), 100L));
        }
    }

    // ===== Callback Verification =====

    @Nested
    @DisplayName("Callback verification — signature, amount, status")
    class CallbackVerificationTests {

        @Test
        @DisplayName("Valid signature + correct amount + success → payment SUCCESS, subscription ACTIVE")
        void testValidCallback_success() {
            Payment payment = createPendingPayment();
            when(paymentRepository.findByTransactionUuid(PRN)).thenReturn(Optional.of(payment));
            when(signatureService.verifySignature(anyString(), anyString(), anyString())).thenReturn(true);

            PaymentResponse response = service.handleCallback(
                    PRN, PID, "success", "6100.87", "UID123", "BID456", "valid-sig");

            assertEquals("SUCCESS", response.getStatus());
            assertEquals("UID123", response.getGatewayTransactionId());
            assertEquals("BID456", response.getGatewayRefId());

            verify(invoiceService).generateInvoiceNumber();
            verify(subscriptionRepository).save(argThat(s ->
                    s.getStatus() == SubscriptionStatus.ACTIVE &&
                    s.getVehicleLimitSnapshot() == 120 // 10 × 12
            ));
            verify(paymentExpenseSyncService).createSubscriptionExpense(any(), any());
        }

        @Test
        @DisplayName("Invalid signature → VERIFICATION_FAILED")
        void testInvalidSignature_verificationFailed() {
            Payment payment = createPendingPayment();
            when(paymentRepository.findByTransactionUuid(PRN)).thenReturn(Optional.of(payment));
            when(signatureService.verifySignature(anyString(), anyString(), anyString())).thenReturn(false);

            PaymentResponse response = service.handleCallback(
                    PRN, PID, "success", "6100.87", "UID123", "BID456", "bad-sig");

            assertEquals("VERIFICATION_FAILED", response.getStatus());
            assertTrue(response.getFailureReason().contains("Signature"));
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Wrong merchant code → VERIFICATION_FAILED")
        void testWrongMerchantCode_verificationFailed() {
            Payment payment = createPendingPayment();
            when(paymentRepository.findByTransactionUuid(PRN)).thenReturn(Optional.of(payment));
            when(signatureService.verifySignature(anyString(), anyString(), anyString())).thenReturn(true);

            PaymentResponse response = service.handleCallback(
                    PRN, "WRONG_PID", "success", "6100.87", "UID123", "BID456", "sig");

            assertEquals("VERIFICATION_FAILED", response.getStatus());
            assertTrue(response.getFailureReason().contains("Merchant code"));
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Wrong amount → VERIFICATION_FAILED")
        void testWrongAmount_verificationFailed() {
            Payment payment = createPendingPayment();
            when(paymentRepository.findByTransactionUuid(PRN)).thenReturn(Optional.of(payment));
            when(signatureService.verifySignature(anyString(), anyString(), anyString())).thenReturn(true);

            PaymentResponse response = service.handleCallback(
                    PRN, PID, "success", "500.00", "UID123", "BID456", "sig");

            assertEquals("VERIFICATION_FAILED", response.getStatus());
            assertTrue(response.getFailureReason().contains("Amount mismatch"));
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Non-success status → payment FAILED")
        void testNonSuccessStatus_paymentFailed() {
            Payment payment = createPendingPayment();
            when(paymentRepository.findByTransactionUuid(PRN)).thenReturn(Optional.of(payment));
            when(signatureService.verifySignature(anyString(), anyString(), anyString())).thenReturn(true);

            PaymentResponse response = service.handleCallback(
                    PRN, PID, "failed", "6100.87", "UID123", "BID456", "sig");

            assertEquals("FAILED", response.getStatus());
            verify(subscriptionRepository, never()).save(any());
        }
    }

    // ===== Idempotency =====

    @Nested
    @DisplayName("Idempotency — duplicate callbacks")
    class IdempotencyTests {

        @Test
        @DisplayName("Already SUCCESS → no reprocessing")
        void testAlreadySuccess_noReprocessing() {
            Payment payment = createPendingPayment();
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setInvoiceNumber("INV-2026-00001");
            payment.setSubscriptionId(1L);
            when(paymentRepository.findByTransactionUuid(PRN)).thenReturn(Optional.of(payment));
            when(signatureService.verifySignature(anyString(), anyString(), anyString())).thenReturn(true);

            PaymentResponse response = service.handleCallback(
                    PRN, PID, "success", "6100.87", "UID123", "BID456", "sig");

            assertEquals("SUCCESS", response.getStatus());
            verify(invoiceService, never()).generateInvoiceNumber();
            verify(subscriptionRepository, never()).save(any());
            verify(paymentExpenseSyncService, never()).createSubscriptionExpense(any(), any());
        }
    }

    // ===== Payment Not Found =====

    @Test
    @DisplayName("Payment not found → PaymentException")
    void testPaymentNotFound_throws() {
        when(paymentRepository.findByTransactionUuid(PRN)).thenReturn(Optional.empty());

        assertThrows(PaymentException.class,
                () -> service.handleCallback(PRN, PID, "success", "6100.87", "UID", "BID", "sig"));
    }

    // ===== Get Payment By PRN =====

    @Test
    @DisplayName("getPaymentByPrn → returns payment response")
    void testGetPaymentByPrn() {
        Payment payment = createPendingPayment();
        when(paymentRepository.findByTransactionUuid(PRN)).thenReturn(Optional.of(payment));

        PaymentResponse response = service.getPaymentByPrn(PRN);

        assertNotNull(response);
        assertEquals(PRN, response.getTransactionUuid());
    }

    @Test
    @DisplayName("getPaymentByPrn not found → PaymentException")
    void testGetPaymentByPrnNotFound_throws() {
        when(paymentRepository.findByTransactionUuid(PRN)).thenReturn(Optional.empty());

        assertThrows(PaymentException.class, () -> service.getPaymentByPrn(PRN));
    }
}

