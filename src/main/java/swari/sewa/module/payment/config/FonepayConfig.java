package swari.sewa.module.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "fonepay")
public class FonepayConfig {

    private String merchantCodePid;
    private String merchantSecretKey;
    private String paymentUrl;
    private String backendReturnUrl;
    private String frontendSuccessUrl;
    private String frontendFailureUrl;
}
