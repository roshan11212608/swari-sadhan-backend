package swari.sewa.module.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "esewa")
public class EsewaConfig {

    private String environment;
    private String productCode;
    private String secretKey;
    private String paymentUrl;
    private String statusUrl;
    private String backendSuccessUrl;
    private String backendFailureUrl;
    private String frontendSuccessUrl;
    private String frontendFailureUrl;
}
