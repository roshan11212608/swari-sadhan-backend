package swari.sewa.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Validates that required production configuration is present at startup.
 * <p>
 * In the {@code prod} profile, the application fails fast if critical
 * environment variables are missing, rather than silently degrading to
 * local/dev behavior or using insecure test credentials.
 */
@Configuration
@Profile("prod")
@Slf4j
public class ProductionConfigValidator {

    @Value("${r2.endpoint:}")
    private String r2Endpoint;

    @Value("${r2.access-key:}")
    private String r2AccessKey;

    @Value("${r2.secret-key:}")
    private String r2SecretKey;

    @Value("${r2.public-url:}")
    private String r2PublicUrl;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${app.frontend-base-url:}")
    private String frontendBaseUrl;

    @Value("${app.backend-base-url:}")
    private String backendBaseUrl;

    @Value("${spring.mail.username:}")
    private String brevoSmtpUsername;

    @Value("${brevo.sender-email:}")
    private String brevoSenderEmail;

    @Value("${esewa.product-code:}")
    private String esewaProductCode;

    @Value("${esewa.secret-key:}")
    private String esewaSecretKey;

    @Value("${esewa.payment-url:}")
    private String esewaPaymentUrl;

    @Value("${fonepay.merchant-code-pid:}")
    private String fonepayMerchantCode;

    @Value("${fonepay.merchant-secret-key:}")
    private String fonepaySecretKey;

    @Value("${fonepay.payment-url:}")
    private String fonepayPaymentUrl;

    @PostConstruct
    public void validate() {
        // R2 storage — mandatory
        require("R2_ENDPOINT", r2Endpoint, "Cloudflare R2 storage endpoint");
        require("R2_ACCESS_KEY", r2AccessKey, "Cloudflare R2 access key");
        require("R2_SECRET_KEY", r2SecretKey, "Cloudflare R2 secret key");
        require("R2_PUBLIC_URL", r2PublicUrl, "Cloudflare R2 public URL");

        // JWT — mandatory
        require("JWT_SECRET", jwtSecret, "JWT signing secret");

        // App URLs — mandatory
        require("APP_FRONTEND_URL", frontendBaseUrl, "Frontend base URL for redirects/emails");
        require("APP_BACKEND_URL", backendBaseUrl, "Backend base URL for payment callbacks");

        // Brevo SMTP — optional (warn if missing, email features won't work)
        warnIfMissing("BREVO_SMTP_USERNAME", brevoSmtpUsername, "Brevo SMTP username");
        warnIfMissing("BREVO_SENDER_EMAIL", brevoSenderEmail, "Brevo verified sender email");

        // eSewa — optional (warn if missing, eSewa payments won't work)
        warnIfMissing("ESEWA_PRODUCT_CODE", esewaProductCode, "eSewa production product code");
        warnIfMissing("ESEWA_SECRET_KEY", esewaSecretKey, "eSewa production secret key");
        warnIfMissing("ESEWA_PAYMENT_URL", esewaPaymentUrl, "eSewa production payment URL");

        // Fonepay — optional (warn if missing, Fonepay payments won't work)
        warnIfMissing("FONEPAY_MERCHANT_CODE", fonepayMerchantCode, "Fonepay merchant code");
        warnIfMissing("FONEPAY_SECRET_KEY", fonepaySecretKey, "Fonepay merchant secret key");
        warnIfMissing("FONEPAY_PAYMENT_URL", fonepayPaymentUrl, "Fonepay production payment URL");

        log.info("Production configuration validated: R2 storage, JWT, and app URLs are configured.");
    }

    private void require(String envVar, String value, String description) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                String.format(
                    "FATAL: %s is not set. %s is required in production. " +
                    "Set this environment variable on the deployment platform.",
                    envVar, description
                )
            );
        }
    }

    private void warnIfMissing(String envVar, String value, String description) {
        if (value == null || value.isBlank()) {
            log.warn("WARNING: {} is not set. {} — this feature will not work until it is configured.",
                envVar, description);
        }
    }
}
