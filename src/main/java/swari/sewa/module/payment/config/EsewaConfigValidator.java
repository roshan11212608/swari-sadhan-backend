package swari.sewa.module.payment.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EsewaConfigValidator {

    private final EsewaConfig esewaConfig;

    @PostConstruct
    public void validate() {
        String env = esewaConfig.getEnvironment();
        log.info("eSewa environment: {}", env);

        if (env == null || env.isEmpty()) {
            throw new IllegalStateException("esewa.environment is not configured");
        }

        // Validate required fields
        if (esewaConfig.getProductCode() == null || esewaConfig.getProductCode().isEmpty()) {
            throw new IllegalStateException("esewa.product-code is not configured");
        }
        if (esewaConfig.getSecretKey() == null || esewaConfig.getSecretKey().isEmpty()) {
            throw new IllegalStateException("esewa.secret-key is not configured. Set ESEWA_SECRET_KEY environment variable.");
        }
        if (esewaConfig.getPaymentUrl() == null || esewaConfig.getPaymentUrl().isEmpty()) {
            throw new IllegalStateException("esewa.payment-url is not configured");
        }
        if (esewaConfig.getStatusUrl() == null || esewaConfig.getStatusUrl().isEmpty()) {
            throw new IllegalStateException("esewa.status-url is not configured");
        }
        if (esewaConfig.getBackendSuccessUrl() == null || esewaConfig.getBackendSuccessUrl().isEmpty()) {
            throw new IllegalStateException("esewa.backend-success-url is not configured");
        }
        if (esewaConfig.getBackendFailureUrl() == null || esewaConfig.getBackendFailureUrl().isEmpty()) {
            throw new IllegalStateException("esewa.backend-failure-url is not configured");
        }
        if (esewaConfig.getFrontendSuccessUrl() == null || esewaConfig.getFrontendSuccessUrl().isEmpty()) {
            throw new IllegalStateException("esewa.frontend-success-url is not configured");
        }
        if (esewaConfig.getFrontendFailureUrl() == null || esewaConfig.getFrontendFailureUrl().isEmpty()) {
            throw new IllegalStateException("esewa.frontend-failure-url is not configured");
        }

        // Production-specific validation — never allow EPAYTEST in production
        if ("production".equalsIgnoreCase(env)) {
            if ("EPAYTEST".equalsIgnoreCase(esewaConfig.getProductCode())) {
                throw new IllegalStateException(
                    "CRITICAL: esewa.product-code is EPAYTEST but environment is 'production'. "
                    + "This must never happen — production must use the real merchant code.");
            }
            if (esewaConfig.getPaymentUrl() != null && esewaConfig.getPaymentUrl().contains("rc-epay")) {
                throw new IllegalStateException(
                    "CRITICAL: esewa.payment-url points to the UAT endpoint (rc-epay) but environment is 'production'.");
            }
            if (esewaConfig.getStatusUrl() != null && esewaConfig.getStatusUrl().contains("rc.esewa")) {
                throw new IllegalStateException(
                    "CRITICAL: esewa.status-url points to the UAT endpoint (rc.esewa) but environment is 'production'.");
            }
            log.info("eSewa production configuration validated — merchant code: {}", esewaConfig.getProductCode());
        } else if ("uat".equalsIgnoreCase(env)) {
            log.info("eSewa UAT configuration validated — product code: {}", esewaConfig.getProductCode());
        } else {
            log.warn("eSewa environment '{}' is not 'uat' or 'production' — proceeding anyway", env);
        }
    }
}
