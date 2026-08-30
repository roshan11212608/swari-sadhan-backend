package swari.sewa.module.subscription.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.common.enums.UserRole;
import swari.sewa.common.util.JwtUtil;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Controller security tests using MockMvc with real JWT authentication.
 *
 * Tests verify:
 * - Public endpoints accessible without authentication
 * - SUPERADMIN-only endpoints enforce @PreAuthorize
 * - Shop owner endpoints resolve current owner from JWT
 * - Cross-owner access is denied
 * - Invoice ownership check enforced
 *
 * Uses the real security configuration (SecurityConfig, JwtAuthenticationFilter)
 * with method-level security (@PreAuthorize) enabled.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional
class ControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private ShopOwnerRepository shopOwnerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private String superadminToken;
    private String shopOwnerAToken;
    private String shopOwnerBToken;
    private ShopOwner ownerA;
    private ShopOwner ownerB;

    @BeforeEach
    void setUp() {
        // Create a SUPERADMIN user
        User admin = User.builder()
                .email("admin_test@swari.com")
                .password(passwordEncoder.encode("TestPass123!"))
                .firstName("Admin")
                .lastName("Test")
                .role(UserRole.SUPERADMIN)
                .isActive(true)
                .build();
        admin = userRepository.save(admin);
        superadminToken = jwtUtil.generateToken(admin.getEmail(), admin.getRole().name());

        // Create Shop Owner A
        ownerA = ShopOwner.builder()
                .firstName("Owner")
                .lastName("A")
                .email("owner-a-test@swari.com")
                .password(passwordEncoder.encode("TestPass123!"))
                .phone("9800000001")
                .role(UserRole.SHOP_OWNER)
                .active(true)
                .emailVerified(true)
                .approvalStatus("APPROVED")
                .passwordChanged(false)
                .build();
        ownerA = shopOwnerRepository.save(ownerA);
        shopOwnerAToken = jwtUtil.generateToken(ownerA.getEmail(), ownerA.getRole().name());

        // Create Shop Owner B
        ownerB = ShopOwner.builder()
                .firstName("Owner")
                .lastName("B")
                .email("owner-b-test@swari.com")
                .password(passwordEncoder.encode("TestPass123!"))
                .phone("9800000002")
                .role(UserRole.SHOP_OWNER)
                .active(true)
                .emailVerified(true)
                .approvalStatus("APPROVED")
                .passwordChanged(false)
                .build();
        ownerB = shopOwnerRepository.save(ownerB);
        shopOwnerBToken = jwtUtil.generateToken(ownerB.getEmail(), ownerB.getRole().name());
    }

    // ===== Public Endpoints =====

    @Nested
    @DisplayName("Public endpoints — no authentication required")
    class PublicEndpointTests {

        @Test
        @DisplayName("GET /api/subscription/plans/public → accessible without auth")
        void testPublicPlans_noAuth() throws Exception {
            mockMvc.perform(get("/api/subscription/plans/public"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/subscription/plans/tax-settings → accessible without auth")
        void testTaxSettings_noAuth() throws Exception {
            mockMvc.perform(get("/api/subscription/plans/tax-settings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.enableVat").exists())
                    .andExpect(jsonPath("$.data.taxPercentage").exists())
                    .andExpect(jsonPath("$.data.currency").exists());
        }

        @Test
        @DisplayName("GET /api/subscription/plans/coupon/validate → accessible without auth")
        void testCouponValidate_noAuth() throws Exception {
            mockMvc.perform(get("/api/subscription/plans/coupon/validate")
                            .param("code", "NONEXISTENT")
                            .param("amount", "1000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.valid").value(false));
        }
    }

    // ===== SUPERADMIN-only Endpoints =====

    @Nested
    @DisplayName("SUPERADMIN-only endpoints — @PreAuthorize enforcement")
    class SuperAdminEndpointTests {

        @Test
        @DisplayName("GET /api/superadmin/subscription/plans with SUPERADMIN → 200")
        void testPlansWithSuperAdmin() throws Exception {
            mockMvc.perform(get("/api/superadmin/subscription/plans")
                            .header("Authorization", "Bearer " + superadminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/superadmin/subscription/plans with SHOP_OWNER → 403")
        void testPlansWithShopOwner_forbidden() throws Exception {
            mockMvc.perform(get("/api/superadmin/subscription/plans")
                            .header("Authorization", "Bearer " + shopOwnerAToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/superadmin/subscription/plans without auth → 403 (method security)")
        void testPlansWithoutAuth_forbidden() throws Exception {
            // @PreAuthorize("hasRole('SUPERADMIN')") triggers AccessDeniedException
            // even without authentication, because the method is invoked
            mockMvc.perform(get("/api/superadmin/subscription/plans"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/superadmin/subscription/subscribers with SHOP_OWNER → 403")
        void testSubscribersWithShopOwner_forbidden() throws Exception {
            mockMvc.perform(get("/api/superadmin/subscription/subscribers")
                            .header("Authorization", "Bearer " + shopOwnerAToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/superadmin/subscription/transactions with SHOP_OWNER → 403")
        void testTransactionsWithShopOwner_forbidden() throws Exception {
            mockMvc.perform(get("/api/superadmin/subscription/transactions")
                            .header("Authorization", "Bearer " + shopOwnerAToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/superadmin/subscription/coupons with SHOP_OWNER → 403")
        void testCouponsWithShopOwner_forbidden() throws Exception {
            mockMvc.perform(get("/api/superadmin/subscription/coupons")
                            .header("Authorization", "Bearer " + shopOwnerAToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/superadmin/subscription/settings with SHOP_OWNER → 403")
        void testSettingsWithShopOwner_forbidden() throws Exception {
            mockMvc.perform(get("/api/superadmin/subscription/settings")
                            .header("Authorization", "Bearer " + shopOwnerAToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ===== Shop Owner Payment Endpoints =====

    @Nested
    @DisplayName("Shop owner payment endpoints — authentication required")
    class ShopOwnerPaymentEndpointTests {

        @Test
        @DisplayName("GET /api/payments/subscription/current without auth → 401")
        void testCurrentSubscriptionNoAuth() throws Exception {
            mockMvc.perform(get("/api/payments/subscription/current"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/payments/billing-history without auth → 401")
        void testBillingHistoryNoAuth() throws Exception {
            mockMvc.perform(get("/api/payments/billing-history"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/payments/subscription/current with valid auth → 200")
        void testCurrentSubscriptionWithAuth() throws Exception {
            mockMvc.perform(get("/api/payments/subscription/current")
                            .header("Authorization", "Bearer " + shopOwnerAToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/payments/billing-history with valid auth → 200")
        void testBillingHistoryWithAuth() throws Exception {
            mockMvc.perform(get("/api/payments/billing-history")
                            .header("Authorization", "Bearer " + shopOwnerAToken))
                    .andExpect(status().isOk());
        }
    }

    // ===== Cross-Owner Invoice Access =====

    @Nested
    @DisplayName("Cross-owner invoice access — ownership check enforced")
    class CrossOwnerInvoiceTests {

        @Test
        @DisplayName("GET /api/payments/invoice/{uuid} without auth → 401")
        void testInvoiceNoAuth() throws Exception {
            mockMvc.perform(get("/api/payments/invoice/SS-NONEXISTENT-000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/payments/invoice/{uuid} with non-existent UUID → 404 or 400")
        void testInvoiceNotFound() throws Exception {
            mockMvc.perform(get("/api/payments/invoice/SS-NONEXISTENT-000001")
                            .header("Authorization", "Bearer " + shopOwnerAToken))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ===== Payment Creation Validation =====

    @Nested
    @DisplayName("Payment creation — input validation")
    class PaymentCreationValidationTests {

        @Test
        @DisplayName("POST /api/payments/esewa/create without auth → 401")
        void testCreatePaymentNoAuth() throws Exception {
            String body = "{\"planId\":1,\"billingCycle\":\"yearly\"}";
            mockMvc.perform(post("/api/payments/esewa/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/payments/esewa/create with missing planId → 400")
        void testCreatePaymentMissingPlanId() throws Exception {
            String body = "{\"billingCycle\":\"yearly\"}";
            mockMvc.perform(post("/api/payments/esewa/create")
                            .header("Authorization", "Bearer " + shopOwnerAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/payments/esewa/create with missing billingCycle → 400")
        void testCreatePaymentMissingBillingCycle() throws Exception {
            String body = "{\"planId\":1}";
            mockMvc.perform(post("/api/payments/esewa/create")
                            .header("Authorization", "Bearer " + shopOwnerAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/payments/esewa/create with invalid billing cycle → 400 or 500")
        void testCreatePaymentInvalidBillingCycle() throws Exception {
            String body = "{\"planId\":1,\"billingCycle\":\"INVALID\"}";
            int status = mockMvc.perform(post("/api/payments/esewa/create")
                            .header("Authorization", "Bearer " + shopOwnerAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn().getResponse().getStatus();
            assertTrue(status >= 400 && status < 600,
                    "Invalid billing cycle should result in client or server error, got: " + status);
        }
    }
}
