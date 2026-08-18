package swari.sewa.module.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.common.dto.SignupRequest;
import swari.sewa.common.enums.UserRole;
import swari.sewa.common.exception.MobileNumberAlreadyExistsException;
import swari.sewa.common.exception.OtpRateLimitException;
import swari.sewa.common.exception.OtpVerificationException;
import swari.sewa.module.auth.dto.OtpMessageResponse;
import swari.sewa.module.auth.dto.OtpSignupCompleteRequest;
import swari.sewa.module.auth.dto.VerifyOtpRequest;
import swari.sewa.module.auth.dto.VerifyOtpResponse;
import swari.sewa.module.auth.entity.SignupOtp;
import swari.sewa.module.auth.repository.SignupOtpRepository;
import swari.sewa.module.auth.service.BrevoSmsService;
import swari.sewa.module.auth.service.OtpSignupService;
import swari.sewa.module.user.dto.UserDto;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.user.service.UserService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Production OTP signup implementation.
 *
 * <p>Security properties:
 * <ul>
 *   <li>6-digit OTP generated with {@link SecureRandom}.</li>
 *   <li>OTP stored only as a BCrypt hash.</li>
 *   <li>5-minute OTP expiry; single-use.</li>
 *   <li>Max 5 verification attempts per OTP.</li>
 *   <li>60-second resend cooldown; max 5 resends per number.</li>
 *   <li>Issuing a new OTP invalidates the previous one for that number.</li>
 *   <li>Verification token is a random UUID stored as a SHA-256 hash;
 *       the backend re-validates it (and the verified flag) at signup time.</li>
 *   <li>The raw OTP is never logged or returned in any API response.</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OtpSignupServiceImpl implements OtpSignupService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MINUTES = 5;
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_RESENDS = 5;
    private static final int VERIFICATION_TOKEN_TTL_MINUTES = 10;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SignupOtpRepository signupOtpRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final BrevoSmsService brevoSmsService;

    @Override
    public OtpMessageResponse sendOtp(String rawMobileNumber) {
        String mobile = normalizeMobile(rawMobileNumber);

        // Prevent duplicate-account creation: if an account already uses this
        // mobile number, do not send an OTP.
        if (userRepository.findByPhoneNumber(mobile).isPresent()) {
            throw new MobileNumberAlreadyExistsException(
                    "An account with this mobile number already exists.");
        }

        Optional<SignupOtp> existingOpt = signupOtpRepository
                .findTopByMobileNumberOrderByCreatedAtDesc(mobile);
        LocalDateTime now = LocalDateTime.now();

        if (existingOpt.isPresent()) {
            SignupOtp existing = existingOpt.get();
            // Enforce resend cooldown against the most recent record.
            if (existing.getLastSentAt() != null
                    && existing.getLastSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)
                    && !existing.isVerified()) {
                long wait = java.time.Duration.between(now,
                        existing.getLastSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS)).getSeconds();
                throw new OtpRateLimitException(
                        "Please wait " + wait + "s before requesting another OTP.");
            }
            // Cap total resends for a number.
            if (existing.getResendCount() >= MAX_RESENDS && !existing.isVerified()) {
                throw new OtpRateLimitException(
                        "Too many OTP requests. Please try again later.");
            }
        }

        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);

        SignupOtp record = existingOpt.map(o -> {
            // Reuse the row but rotate the OTP and reset attempt state.
            o.setOtpHash(otpHash);
            o.setExpiresAt(now.plusMinutes(OTP_TTL_MINUTES));
            o.setVerificationAttempts(0);
            o.setResendCount(o.getResendCount() + 1);
            o.setLastSentAt(now);
            o.setVerified(false);
            o.setVerificationTokenHash(null);
            o.setTokenExpiresAt(null);
            o.setUsedAt(null);
            return o;
        }).orElseGet(() -> SignupOtp.builder()
                .mobileNumber(mobile)
                .otpHash(otpHash)
                .expiresAt(now.plusMinutes(OTP_TTL_MINUTES))
                .verificationAttempts(0)
                .resendCount(1)
                .lastSentAt(now)
                .verified(false)
                .build());

        signupOtpRepository.save(record);

        String content = "Swari Sadhan: Your verification code is " + otp
                + ". Valid for " + OTP_TTL_MINUTES + " minutes. Do not share it.";
        boolean sent = brevoSmsService.sendSms(mobile, content);
        if (!sent) {
            // Brevo failed (e.g. key missing in prod, API down). Surface a clear
            // error so the user can retry; do not leak the OTP.
            throw new RuntimeException(
                    "We couldn't send the OTP right now. Please try again in a moment.");
        }

        return OtpMessageResponse.builder()
                .message("OTP sent successfully")
                .resendCooldownSeconds(RESEND_COOLDOWN_SECONDS)
                .maskedMobile(maskMobile(mobile))
                .build();
    }

    @Override
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String mobile = normalizeMobile(request.getMobileNumber());
        String submittedOtp = request.getOtp().trim();

        SignupOtp record = signupOtpRepository
                .findTopByMobileNumberOrderByCreatedAtDesc(mobile)
                .orElseThrow(() -> new OtpVerificationException(
                        "No OTP was requested for this mobile number. Please request a new one."));

        if (record.isVerified() && record.getUsedAt() == null
                && record.getVerificationTokenHash() != null) {
            // Already verified and token still valid -> re-issue the same token
            // (idempotent) so a page refresh doesn't lock the user out.
            if (record.getTokenExpiresAt() != null
                    && record.getTokenExpiresAt().isAfter(LocalDateTime.now())) {
                // We cannot return the original raw token (we only store its hash).
                // Issue a fresh token bound to the same verified record instead.
                String newToken = UUID.randomUUID().toString();
                record.setVerificationTokenHash(hashSha256(newToken));
                record.setTokenExpiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_TOKEN_TTL_MINUTES));
                signupOtpRepository.save(record);
                return VerifyOtpResponse.builder()
                        .message("Mobile number already verified")
                        .signupVerificationToken(newToken)
                        .maskedMobile(maskMobile(mobile))
                        .build();
            }
        }

        if (record.isVerified()) {
            throw new OtpVerificationException(
                    "This OTP has already been used. Please request a new one.");
        }

        if (record.getUsedAt() != null) {
            throw new OtpVerificationException(
                    "This OTP session is no longer valid. Please request a new one.");
        }

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OtpVerificationException(
                    "The OTP has expired. Please request a new one.");
        }

        if (record.getVerificationAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
            // Invalidate the record to force a fresh OTP.
            record.setUsedAt(LocalDateTime.now());
            signupOtpRepository.save(record);
            throw new OtpVerificationException(
                    "Too many incorrect attempts. Please request a new OTP.");
        }

        // Increment attempt counter BEFORE the match check to prevent
        // timing-based attempt enumeration.
        record.setVerificationAttempts(record.getVerificationAttempts() + 1);
        signupOtpRepository.save(record);

        if (!passwordEncoder.matches(submittedOtp, record.getOtpHash())) {
            int remaining = MAX_VERIFICATION_ATTEMPTS - record.getVerificationAttempts();
            throw new OtpVerificationException(
                    remaining > 0
                            ? "Incorrect OTP. " + remaining + " attempt(s) remaining."
                            : "Incorrect OTP. Please request a new one.");
        }

        // Success: mark verified and issue a single-use verification token.
        String verificationToken = UUID.randomUUID().toString();
        record.setVerified(true);
        record.setVerificationTokenHash(hashSha256(verificationToken));
        record.setTokenExpiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_TOKEN_TTL_MINUTES));
        signupOtpRepository.save(record);

        return VerifyOtpResponse.builder()
                .message("Mobile number verified successfully")
                .signupVerificationToken(verificationToken)
                .maskedMobile(maskMobile(mobile))
                .build();
    }

    @Override
    public UserDto completeSignup(OtpSignupCompleteRequest request) {
        String mobile = normalizeMobile(request.getMobileNumber());
        String token = request.getSignupVerificationToken().trim();

        // Server-side proof: the OTP record must be verified, unused, and the
        // token hash must match. We never trust a frontend flag.
        SignupOtp record = signupOtpRepository
                .findValidVerified(mobile, LocalDateTime.now())
                .orElseThrow(() -> new OtpVerificationException(
                        "Mobile number verification is missing or expired. Please verify again."));

        if (!hashSha256(token).equals(record.getVerificationTokenHash())) {
            throw new OtpVerificationException("Invalid verification token. Please verify again.");
        }

        // Prevent duplicate accounts at the final step as well.
        if (userRepository.findByPhoneNumber(mobile).isPresent()) {
            // Consume the token so it can't be reused.
            record.setUsedAt(LocalDateTime.now());
            signupOtpRepository.save(record);
            throw new MobileNumberAlreadyExistsException(
                    "An account with this mobile number already exists.");
        }

        validatePassword(request.getPassword());

        // Consume the verification token (single-use).
        record.setUsedAt(LocalDateTime.now());
        signupOtpRepository.save(record);

        // Derive first/last name from the provided full name.
        String trimmedName = request.getName().trim();
        String firstName;
        String lastName;
        int spaceIdx = trimmedName.indexOf(' ');
        if (spaceIdx > 0) {
            firstName = trimmedName.substring(0, spaceIdx).trim();
            lastName = trimmedName.substring(spaceIdx + 1).trim();
        } else {
            firstName = trimmedName;
            lastName = trimmedName;
        }

        // Public users sign up with mobile number only — no email is generated.
        // The email column is nullable for this case.
        SignupRequest signupRequest = SignupRequest.builder()
                .email(null)
                .password(request.getPassword())
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(mobile)
                .role(UserRole.PUBLIC)
                .build();

        UserDto created = userService.createUser(signupRequest);

        // Generate a human-friendly customer code (DDMMYYYY-NNN) and mark
        // the mobile as verified.
        Optional<User> saved = userRepository.findByPhoneNumber(mobile);
        saved.ifPresent(u -> {
            u.setIsEmailVerified(true);
            u.setCustomerCode(generateCustomerCode());
            userRepository.save(u);
            created.setCustomerCode(u.getCustomerCode());
        });

        log.info("Public user signed up via OTP for mobile {} (code: {})",
                maskMobile(mobile), created.getCustomerCode());
        return created;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Normalize to the canonical international form {@code +9779XXXXXXXXX}.
     * Accepts {@code 98XXXXXXXX}, {@code 97798XXXXXXXX}, {@code +97798XXXXXXXX}.
     */
    static String normalizeMobile(String raw) {
        if (raw == null) throw new IllegalArgumentException("Mobile number is required");
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("977")) digits = digits.substring(3);
        // Nepal mobile numbers are 10 digits starting with 98/97/96.
        if (digits.length() == 10 && (digits.startsWith("98")
                || digits.startsWith("97") || digits.startsWith("96"))) {
            return "+977" + digits;
        }
        throw new IllegalArgumentException(
                "Enter a valid Nepal mobile number (e.g. +97798XXXXXXXX).");
    }

    private String generateOtp() {
        int max = (int) Math.pow(10, OTP_LENGTH); // 1_000_000
        int min = max / 10;                        // 100_000
        int value = SECURE_RANDOM.nextInt(max - min) + min;
        return String.valueOf(value);
    }

    /**
     * Generates a unique, human-friendly customer code in the format
     * {@code DDMMYYYY-NNN}, where NNN is a zero-padded daily sequence.
     * Example: the first customer created on 12 Aug 2026 gets {@code 12082026-001}.
     */
    private String generateCustomerCode() {
        java.time.LocalDate today = java.time.LocalDate.now();
        String datePart = String.format("%02d%02d%d",
                today.getDayOfMonth(), today.getMonthValue(), today.getYear());
        String prefix = datePart + "-";

        String maxCode = userRepository.findMaxCustomerCodeByPrefix(prefix);
        int nextSeq = 1;
        if (maxCode != null) {
            try {
                String seqPart = maxCode.substring(prefix.length());
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException ignored) {
                // Corrupt code — start fresh at 1.
            }
        }
        return prefix + String.format("%03d", nextSeq);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (password.length() > 72) {
            throw new IllegalArgumentException("Password must be at most 72 characters.");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain an uppercase letter.");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain a lowercase letter.");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must contain a digit.");
        }
    }

    static String maskMobile(String mobile) {
        // +977 98******12
        String digits = mobile.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return "****";
        String last2 = digits.substring(digits.length() - 2);
        return "+977 " + digits.substring(0, 2) + "******" + last2;
    }

    private static String hashSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
