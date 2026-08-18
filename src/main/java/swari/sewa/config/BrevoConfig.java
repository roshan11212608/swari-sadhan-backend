package swari.sewa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Brevo (Sendinblue) configuration. Values are sourced from environment
 * variables via {@code application.properties} and are never exposed to the
 * frontend.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "brevo")
public class BrevoConfig {

    /** Brevo API key. Required for transactional email and SMS delivery. */
    private String apiKey;

    /** Alphanumeric SMS sender name (max 11 chars, letters/digits only). */
    private String smsSender = "SwariSadhn";

    /** Verified sender email used as the From address for transactional email. */
    private String senderEmail;

    /** Display sender name for transactional email. */
    private String senderName = "Swari Sadhan";

    /** Brevo transactional SMS API endpoint. */
    private String smsApiUrl = "https://api.brevo.com/v3/transactionalSMS/send";

    /** Brevo transactional email API endpoint. */
    private String emailApiUrl = "https://api.brevo.com/v3/smtp/email";
}
