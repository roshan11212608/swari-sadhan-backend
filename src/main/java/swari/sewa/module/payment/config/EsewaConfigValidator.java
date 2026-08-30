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
            log.warn("esewa.environment is not configured — eSewa payments will be disabled");
            return;
        }

        // If the core eSewa merchant credentials are missing, log a warning and skip validation
        if (esewaConfig.getProductCode() == null || esewaConfig.getProductCode().isEmpty()
                || esewaConfig.getSecretKey() == null || esewaConfig.getSecretKey().isEmpty()
                || esewaConfig.getPaymentUrl() == null || esewaConfig.getPaymentUrl().isEmpty()
                || esewaConfig.getStatusUrl() == null || esewaConfig.getStatusUrl().isEmpty()) {
            log.warn("eSewa is not fully configured — eSewa payments will be disabled");
            return;
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
