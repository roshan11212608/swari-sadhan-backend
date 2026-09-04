package swari.sewa.module.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import swari.sewa.config.BrevoConfig;

/**
 * Sends transactional emails (OTP codes, approval/rejection notifications,
 * credentials).
 * <p>
 * Delivery order:
 * <ol>
 *   <li>SMTP via {@link JavaMailSender} (Brevo SMTP relay by default, or any
 *       SMTP server) when {@code spring.mail.username}/{@code password} are
 *       configured. This is the preferred path.</li>
 *   <li>Brevo transactional email REST API, when no SMTP credentials are set
 *       but a Brevo API key and verified sender email are.</li>
 *   <li>Console logging, in dev only, so flows remain testable with no
 *       provider configured.</li>
 * </ol>
 * Credentials come from environment variables or the gitignored
 * {@code application-dev.properties} and are never logged.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final BrevoConfig brevoConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:}")
    private String mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Value("${brevo.sender-name:Swari Sadhan}")
    private String senderName;

    @Value("${brevo.sender-email:}")
    private String senderEmail;

    /**
     * Startup summary of the mail/Brevo configuration. In dev, logs SMTP
     * details; in prod, logs only the Brevo API key <em>prefix</em> and length
     * so a stale key, a wrong key type, or stray whitespace from copy-paste
     * can be spotted without a send attempt. Never logs the secret itself.
     */
    @PostConstruct
    void logMailConfigOnStartup() {
        if (isDevProfile()) {
            logDevMailConfig();
            return;
        }
        // Prod: log a sanitized summary so we can diagnose 401s without
        // exposing secrets.
        boolean smtpConfigured = isMailConfigured();
        boolean brevoApiConfigured = isBrevoEmailConfigured();
        log.info("Email config: SMTP={} BrevoAPI={} senderEmail='{}'",
                smtpConfigured, brevoApiConfigured,
                (senderEmail == null || senderEmail.isBlank()) ? "(not set)" : senderEmail);
        if (!smtpConfigured && !brevoApiConfigured) {
            log.error("No email delivery method configured! Set BREVO_SMTP_USERNAME + "
                    + "BREVO_SMTP_KEY (preferred) or BREVO_API_KEY + BREVO_SENDER_EMAIL.");
            return;
        }
        if (brevoApiConfigured) {
            String apiKey = brevoConfig.getApiKey();
            String keyPrefix = apiKey.contains("-")
                    ? apiKey.substring(0, apiKey.indexOf('-') + 1)
                    : "(no '-' in key)";
            log.info("Brevo API key loaded: prefix={} length={} (key type: {})",
                    keyPrefix, apiKey.length(),
                    apiKey.startsWith("xkeysib-") ? "REST API key"
                            : apiKey.startsWith("xsmtpsib-") ? "SMTP key (WARNING: wrong type for REST API)"
                            : "unknown");
            if (!apiKey.equals(apiKey.trim())) {
                log.error("BREVO_API_KEY has leading/trailing whitespace — this breaks REST API auth.");
            }
        }
        if (smtpConfigured && mailHost.contains("brevo") && mailPassword.startsWith("xkeysib-")) {
            log.warn("spring.mail.password looks like a Brevo REST API key (xkeysib-). "
                    + "SMTP AUTH needs the SMTP key (xsmtpsib-) from Brevo > SMTP & API > SMTP.");
        }
    }

    /** Dev-only detailed mail config log. */
    private void logDevMailConfig() {
        if (!isMailConfigured()) {
            log.warn("Mail credentials are not configured; email OTPs will only be logged.");
            return;
        }
        String keyPrefix = mailPassword.contains("-")
                ? mailPassword.substring(0, mailPassword.indexOf('-') + 1)
                : "(no '-' in value)";
        log.info("Mail configured: host={} port={} user='{}' keyPrefix={} keyLength={}",
                mailHost, mailPort, mailUsername, keyPrefix, mailPassword.length());
        if (!mailPassword.equals(mailPassword.trim()) || !mailUsername.equals(mailUsername.trim())) {
            log.warn("Mail username/password has leading or trailing whitespace - "
                    + ".properties files keep trailing spaces, which breaks SMTP AUTH.");
        }
        if (mailHost.contains("brevo") && mailPassword.startsWith("xkeysib-")) {
            log.warn("spring.mail.password looks like a Brevo REST API key (xkeysib-). "
                    + "SMTP AUTH needs the SMTP key (xsmtpsib-) from Brevo > SMTP & API > SMTP.");
        }
        if (senderEmail == null || senderEmail.isBlank()) {
            log.warn("brevo.sender-email is not set; the SMTP login cannot be used as a From address.");
        }
    }

    /**
     * Send an HTML email via SMTP when configured, otherwise via the Brevo
     * REST API. Falls back to console logging in dev mode when delivery is
     * unavailable.
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!isMailConfigured()) {
            if (isBrevoEmailConfigured()) {
                if (sendViaBrevo(to, subject, htmlBody)) {
                    return;
                }
                if (isDevProfile()) {
                    logDevFallback("Brevo email send failed", to, subject, htmlBody);
                    return;
                }
                throw new RuntimeException("Failed to send email via Brevo");
            }

            if (isDevProfile()) {
                log.warn("===== DEV MODE: Email not sent (mail not configured) =====");
                log.warn("To:      {}", to);
                log.warn("Subject: {}", subject);
                log.warn("Body:    {}", htmlBody.replaceAll("<[^>]+>", " ").trim());
                log.warn("==========================================================");
                return;
            }
            log.error("Mail is not configured; cannot send email to {}", to);
            return;
        }

        String fromEmail = resolveFromAddress();
        if (fromEmail == null) {
            log.error("No usable From address: set brevo.sender-email to a sender verified "
                    + "in Brevo (the SMTP login cannot be used as From). Email to {} not sent.", to);
            if (isDevProfile()) {
                logDevFallback("From address not configured", to, subject, htmlBody);
                return;
            }
            throw new RuntimeException("Failed to send email: sender address not configured");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String fromAddr = senderName + " <" + fromEmail + ">";
            helper.setFrom(fromAddr);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} with subject '{}'", to, subject);
        } catch (Exception e) {
            // Spring wraps SMTP failures in a generic message ("Authentication
            // failed"), so surface the server's own reply from the root cause -
            // that is what identifies a stale key, wrong login or unverified
            // sender. The reply never contains the credential itself.
            log.error("Failed to send email to {} via {}:{} as user '{}': {} | SMTP reply: {}",
                    to, mailHost, mailPort, mailUsername, e.getMessage(), rootCauseMessage(e));
            if (isDevProfile()) {
                log.warn("===== DEV MODE FALLBACK: Email send failed =====");
                log.warn("To:      {}", to);
                log.warn("Subject: {}", subject);
                log.warn("Body:    {}", htmlBody.replaceAll("<[^>]+>", " ").trim());
                log.warn("=================================================");
                return;
            }
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /** Deepest cause message, which for SMTP failures carries the server reply. */
    private static String rootCauseMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    /**
     * Send an HTML email with a file attachment (e.g. PDF invoice).
     *
     * @param to             recipient email
     * @param subject        email subject
     * @param htmlBody       email HTML body
     * @param attachmentName filename for the attachment (e.g. "invoice.pdf")
     * @param attachmentData raw bytes of the attachment
     * @param attachmentMime MIME type of the attachment (e.g. "application/pdf")
     */
    public void sendHtmlEmailWithAttachment(String to, String subject, String htmlBody,
                                            String attachmentName, byte[] attachmentData, String attachmentMime) {
        if (!isMailConfigured()) {
            if (isBrevoEmailConfigured()) {
                // Brevo API attachment support would need a different payload;
                // fall back to plain email without attachment
                log.warn("Brevo REST API does not support attachments in this config; sending email without attachment to {}", to);
                sendViaBrevo(to, subject, htmlBody);
                return;
            }
            if (isDevProfile()) {
                log.warn("===== DEV MODE: Email with attachment not sent (mail not configured) =====");
                log.warn("To: {} | Subject: {} | Attachment: {} ({} bytes)", to, subject, attachmentName, attachmentData.length);
                log.warn("Body: {}", htmlBody.replaceAll("<[^>]+>", " ").trim());
                log.warn("=============================================================================");
                return;
            }
            log.error("Mail is not configured; cannot send email with attachment to {}", to);
            return;
        }

        String fromEmail = resolveFromAddress();
        if (fromEmail == null) {
            log.error("No usable From address for attachment email to {}", to);
            if (isDevProfile()) {
                logDevFallback("From address not configured", to, subject, htmlBody);
                return;
            }
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String fromAddr = senderName + " <" + fromEmail + ">";
            helper.setFrom(fromAddr);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            // Add attachment
            InputStream dataSource = new ByteArrayInputStream(attachmentData);
            helper.addAttachment(attachmentName, new ByteArrayDataSource(dataSource, attachmentMime));

            mailSender.send(message);
            log.info("Email with attachment '{}' sent to {} with subject '{}'", attachmentName, to, subject);
        } catch (Exception e) {
            log.error("Failed to send email with attachment to {} via {}:{} as user '{}': {} | SMTP reply: {}",
                    to, mailHost, mailPort, mailUsername, e.getMessage(), rootCauseMessage(e));
            if (isDevProfile()) {
                log.warn("===== DEV MODE FALLBACK: Attachment email send failed =====");
                log.warn("To: {} | Subject: {} | Attachment: {}", to, subject, attachmentName);
                log.warn("Body: {}", htmlBody.replaceAll("<[^>]+>", " ").trim());
                log.warn("==========================================================");
                return;
            }
            throw new RuntimeException("Failed to send email with attachment", e);
        }
    }

    /**
     * POST the message to the Brevo transactional email API.
     *
     * @return {@code true} when Brevo accepted the message
     */
    private boolean sendViaBrevo(String to, String subject, String htmlBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoConfig.getApiKey());

        Map<String, Object> payload = Map.of(
                "sender", Map.of(
                        "email", brevoConfig.getSenderEmail(),
                        "name", brevoConfig.getSenderName()
                ),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", htmlBody
        );

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    brevoConfig.getEmailApiUrl(),
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent via Brevo to {} with subject '{}'", to, subject);
                return true;
            }
            log.error("Brevo email API returned status {} body='{}' for recipient {}",
                    response.getStatusCode(), safeResponseBody(response.getBody()), to);
            return false;
        } catch (HttpClientErrorException e) {
            // 4xx — Brevo rejects the request (invalid key, unverified sender, etc.)
            // The response body identifies the exact cause. No secrets are in the body.
            log.error("Brevo email API client error for {}: status={} body='{}'",
                    to, e.getStatusCode(), safeResponseBody(e.getResponseBodyAsString()));
            return false;
        } catch (HttpServerErrorException e) {
            // 5xx — Brevo server-side issue
            log.error("Brevo email API server error for {}: status={} body='{}'",
                    to, e.getStatusCode(), safeResponseBody(e.getResponseBodyAsString()));
            return false;
        } catch (RestClientException e) {
            log.error("Failed to send email via Brevo to {}: {}", to, e.getMessage());
            return false;
        }
    }

    /**
     * Truncate and sanitize a Brevo response body for logging. Brevo error
     * bodies are JSON like {@code {"code":"invalid_key","message":"..."}} and
     * never contain the API key itself, but we cap the length to avoid log
     * spam.
     */
    private static String safeResponseBody(String body) {
        if (body == null || body.isBlank()) return "[no body]";
        String trimmed = body.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) + "..." : trimmed;
    }

    /** Log the message body so dev flows remain testable. Never used in prod. */
    private void logDevFallback(String reason, String to, String subject, String htmlBody) {
        log.warn("===== DEV MODE FALLBACK: {} =====", reason);
        log.warn("To:      {}", to);
        log.warn("Subject: {}", subject);
        log.warn("Body:    {}", htmlBody.replaceAll("<[^>]+>", " ").trim());
        log.warn("=================================================");
    }

    /**
     * Resolve the From address: the configured verified sender, or the SMTP
     * login when that login is a real mailbox. Brevo SMTP relay logins
     * ({@code xxxx@smtp-brevo.com}) are not valid From addresses, so
     * {@code null} is returned in that case.
     */
    private String resolveFromAddress() {
        if (senderEmail != null && !senderEmail.isBlank()) {
            return senderEmail.trim();
        }
        String login = mailUsername == null ? "" : mailUsername.trim().toLowerCase();
        if (login.endsWith("@smtp-brevo.com") || login.endsWith("@smtp-sendinblue.com")) {
            return null;
        }
        return login.isBlank() ? null : mailUsername.trim();
    }

    /** Brevo email requires both an API key and a verified sender address. */
    private boolean isBrevoEmailConfigured() {
        return brevoConfig.getApiKey() != null && !brevoConfig.getApiKey().isBlank()
                && brevoConfig.getSenderEmail() != null && !brevoConfig.getSenderEmail().isBlank();
    }

    private boolean isMailConfigured() {
        return mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank()
                && !mailUsername.equals("your-email@gmail.com")
                && !mailPassword.equals("your-app-password");
    }

    private boolean isDevProfile() {
        return activeProfile != null && activeProfile.contains("dev");
    }
}
