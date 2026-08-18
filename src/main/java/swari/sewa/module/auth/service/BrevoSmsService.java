package swari.sewa.module.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import swari.sewa.config.BrevoConfig;

import java.util.Map;

/**
 * Sends transactional SMS (OTP) via the Brevo API.
 * <p>
 * The API key lives only in backend configuration and is never logged.
 * Failures are reported without leaking the OTP value.
 *
 * <p><b>Dev-mode fallback:</b> when no Brevo API key is configured AND the
 * active Spring profile is {@code dev}, the SMS content (which includes the
 * OTP) is logged to the console so the flow can be tested locally without a
 * Brevo account. This fallback <b>never</b> activates outside the dev profile,
 * so OTPs are never logged in production.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BrevoSmsService {

    private final BrevoConfig brevoConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    /**
     * Send an SMS containing {@code content} to {@code recipient}.
     *
     * @param recipient mobile number in international format, digits only or
     *                  prefixed with {@code +} (e.g. {@code 97798XXXXXXXX})
     * @param content   the SMS body
     * @return {@code true} if the message was delivered (or dev-mode logged)
     */
    public boolean sendSms(String recipient, String content) {
        // Brevo SMS consumes paid credits, so never call the API in dev. The OTP
        // is logged instead so the mobile flow stays testable at zero cost.
        // Production (any non-dev profile) performs real delivery.
        if (isDevProfile()) {
            log.warn("===== DEV MODE: OTP SMS not sent (paid SMS skipped in dev) =====");
            log.warn("Recipient: {}", recipient);
            log.warn("Content:   {}", content);
            log.warn("===============================================================");
            return true;
        }

        boolean hasKey = brevoConfig.getApiKey() != null && !brevoConfig.getApiKey().isBlank();

        if (!hasKey) {
            log.error("Brevo API key is not configured; cannot send OTP SMS to {}",
                    maskRecipient(recipient));
            return false;
        }

        String normalizedRecipient = recipient.startsWith("+") ? recipient.substring(1) : recipient;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoConfig.getApiKey());

        Map<String, Object> payload = Map.of(
                "recipient", normalizedRecipient,
                "sender", brevoConfig.getSmsSender(),
                "content", content,
                "type", "transactional"
        );

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    brevoConfig.getSmsApiUrl(),
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OTP SMS accepted by Brevo for recipient {}", maskRecipient(recipient));
                // Dev-mode: also log the OTP content so it can be read from the
                // console without relying on actual SMS delivery (useful when
                // the Brevo plan has no SMS credits). NEVER active in production.
                if (isDevProfile()) {
                    log.warn("===== DEV MODE: SMS content (also sent via Brevo) =====");
                    log.warn("Recipient: {}", recipient);
                    log.warn("Content:   {}", content);
                    log.warn("=======================================================");
                }
                return true;
            }
            log.error("Brevo SMS API returned status {} for recipient {}",
                    response.getStatusCode(), maskRecipient(recipient));
            // Dev fallback: if Brevo rejects (e.g. wrong key type / SMTP key used
            // for REST API), log the OTP locally so the flow can still be tested.
            if (isDevProfile()) {
                log.warn("===== DEV MODE FALLBACK: Brevo rejected the request =====");
                log.warn("Recipient: {}", recipient);
                log.warn("Content:   {}", content);
                log.warn("=========================================================");
                return true;
            }
            return false;
        } catch (RestClientException e) {
            log.error("Failed to send OTP SMS via Brevo for recipient {}: {}",
                    maskRecipient(recipient), e.getMessage());
            if (isDevProfile()) {
                log.warn("===== DEV MODE FALLBACK: Brevo call threw an exception =====");
                log.warn("Recipient: {}", recipient);
                log.warn("Content:   {}", content);
                log.warn("===========================================================");
                return true;
            }
            return false;
        }
    }

    private boolean isDevProfile() {
        return activeProfile != null && activeProfile.contains("dev");
    }

    private String maskRecipient(String recipient) {
        if (recipient == null || recipient.length() < 4) return "****";
        return recipient.substring(0, Math.min(4, recipient.length())) + "****" +
                recipient.substring(recipient.length() - 2);
    }
}
