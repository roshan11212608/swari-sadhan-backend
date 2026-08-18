package swari.sewa.module.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.auth.dto.ForgotPasswordRequest;
import swari.sewa.module.auth.dto.ResetPasswordRequest;
import swari.sewa.module.auth.entity.PasswordResetOtp;
import swari.sewa.module.auth.repository.PasswordResetOtpRepository;
import swari.sewa.module.auth.service.BrevoSmsService;
import swari.sewa.module.auth.service.EmailService;
import swari.sewa.module.auth.service.PasswordResetService;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MINUTES = 5;
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final UserRepository userRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final EmailService emailService;
    private final BrevoSmsService brevoSmsService;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${spring.profiles.active:}")
    private String activeProfile;

    @Override
    public String sendResetOtp(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Check that the email belongs to at least one account. We don't reveal
        // to the caller whether the email exists, but we only send an OTP if it
        // does — so we don't waste email quota on unknown addresses.
        boolean exists = userRepository.findByEmail(email).isPresent()
                || shopOwnerRepository.findByEmail(email).isPresent();

        if (!exists) {
            log.info("Password reset requested for unknown email {} — no OTP sent", email);
            throw new IllegalArgumentException("No account found with this email address. Please check and try again.");
        }

        // Rate limit: check last sent time
        passwordResetOtpRepository.findLatestByEmail(email)
                .ifPresent(existing -> {
                    if (existing.getLastSentAt() != null
                            && existing.getLastSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS)
                            .isAfter(LocalDateTime.now())) {
                        throw new IllegalArgumentException("Please wait before requesting another code.");
                    }
                });

        String otp = generateOtp();

        PasswordResetOtp record = PasswordResetOtp.builder()
                .email(email)
                .otpHash(passwordEncoder.encode(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .verificationAttempts(0)
                .resendCount(0)
                .lastSentAt(LocalDateTime.now())
                .verified(false)
                .build();

        passwordResetOtpRepository.save(record);

        // Send the OTP via email
        String subject = "Swari Sadhan - Password Reset Verification Code";
        String htmlBody = "<div style='font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:20px'>"
                + "<h2 style='color:#f97316'>Swari Sadhan</h2>"
                + "<p>You requested a password reset.</p>"
                + "<p>Your verification code is:</p>"
                + "<h1 style='font-size:32px;letter-spacing:5px;color:#f97316'>" + otp + "</h1>"
                + "<p>This code is valid for " + OTP_TTL_MINUTES + " minutes. Do not share it.</p>"
                + "<p style='color:#6b7280;font-size:12px;margin-top:24px'>"
                + "If you did not request a password reset, please ignore this email.</p>"
                + "</div>";
        emailService.sendHtmlEmail(email, subject, htmlBody);

        // Dev-mode fallback: log the OTP so flows remain testable even when
        // email delivery is delayed or blocked. Never active in production.
        if (isDevProfile()) {
            log.warn("===== DEV MODE: Password reset OTP for {} =====", email);
            log.warn("OTP code: {}", otp);
            log.warn("Expires in {} minutes", OTP_TTL_MINUTES);
            log.warn("=================================================");
        }

        log.info("Password reset OTP sent for email {}", email);
        return "A verification code has been sent to your email.";
    }

    @Override
    public String verifyOtpAndResetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();
        String newPassword = request.getNewPassword();

        PasswordResetOtp record = passwordResetOtpRepository.findLatestByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No verification code was requested. Please request a new code."));

        if (record.isVerified() && record.getUsedAt() != null) {
            throw new IllegalArgumentException("This verification code has already been used. Please request a new one.");
        }

        if (record.getVerificationAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
            throw new IllegalArgumentException("Too many incorrect attempts. Please request a new code.");
        }

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("The verification code has expired. Please request a new code.");
        }

        record.setVerificationAttempts(record.getVerificationAttempts() + 1);

        if (!passwordEncoder.matches(otp, record.getOtpHash())) {
            passwordResetOtpRepository.save(record);
            throw new IllegalArgumentException("Incorrect verification code. Please try again.");
        }

        // OTP is valid — mark as used so it can't be reused
        record.setVerified(true);
        record.setUsedAt(LocalDateTime.now());
        passwordResetOtpRepository.save(record);

        // Update the password in whichever table(s) the email exists in.
        // A shop owner may exist in both users and shop_owners; both must be
        // updated so login works regardless of which table is checked first.
        String encodedPassword = passwordEncoder.encode(newPassword);
        boolean updated = false;

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            user.setPassword(encodedPassword);
            userRepository.save(user);
            updated = true;
        }

        ShopOwner shopOwner = shopOwnerRepository.findByEmail(email).orElse(null);
        if (shopOwner != null) {
            shopOwner.setPassword(encodedPassword);
            // If the owner had a forced password change pending, the new
            // password they just chose satisfies it.
            shopOwner.setPasswordChanged(true);
            shopOwnerRepository.save(shopOwner);
            updated = true;
        }

        if (!updated) {
            // Should not happen since sendResetOtp checked, but guard anyway
            throw new IllegalArgumentException("No account found for this email.");
        }

        log.info("Password reset completed for email {}", email);
        return "Your password has been reset successfully. You can now log in with your new password.";
    }

    // ------------------------------------------------------------------
    // Mobile-based reset (for public users who only have a phone number)
    // ------------------------------------------------------------------

    @Override
    public String sendResetOtpByMobile(String mobileNumber) {
        String mobile;
        try {
            mobile = normalizeMobile(mobileNumber);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Enter a valid Nepal mobile number (e.g. +97798XXXXXXXX).");
        }

        // Check that the mobile belongs to at least one account.
        boolean exists = userRepository.findByPhoneNumber(mobile).isPresent()
                || shopOwnerRepository.findByPhone(mobile).isPresent();

        if (!exists) {
            // Also try without the +977 prefix (some records may store just 10 digits)
            String localNum = mobile.replace("+977", "");
            exists = userRepository.findByPhoneNumber(localNum).isPresent()
                    || shopOwnerRepository.findByPhone(localNum).isPresent();
            if (!exists) {
                log.info("Password reset requested for unknown mobile {} — no OTP sent", mobile);
                throw new IllegalArgumentException(
                        "No account found with this mobile number. Please check and try again.");
            }
        }

        // Rate limit
        passwordResetOtpRepository.findLatestByEmail(mobile)
                .ifPresent(existing -> {
                    if (existing.getLastSentAt() != null
                            && existing.getLastSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS)
                            .isAfter(LocalDateTime.now())) {
                        throw new IllegalArgumentException("Please wait before requesting another code.");
                    }
                });

        String otp = generateOtp();

        // Store the OTP using the mobile as the "email" field (the column is
        // generic enough; the mobile is just a lookup key).
        PasswordResetOtp record = PasswordResetOtp.builder()
                .email(mobile)
                .otpHash(passwordEncoder.encode(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .verificationAttempts(0)
                .resendCount(0)
                .lastSentAt(LocalDateTime.now())
                .verified(false)
                .build();

        passwordResetOtpRepository.save(record);

        // Send the OTP via SMS (dev-mode logs the OTP instead of paid SMS)
        String smsContent = "Swari Sadhan: Your password reset verification code is " + otp
                + ". Valid for " + OTP_TTL_MINUTES + " minutes. Do not share it.";
        boolean sent = brevoSmsService.sendSms(mobile, smsContent);
        if (!sent) {
            log.warn("Mobile OTP SMS delivery failed for password reset (mobile: {})", mobile);
        }

        log.info("Password reset OTP sent via SMS for mobile {}", maskMobile(mobile));
        return "A verification code has been sent to your mobile number.";
    }

    @Override
    public String verifyMobileOtpAndResetPassword(String mobileNumber, String otp, String newPassword) {
        String mobile;
        try {
            mobile = normalizeMobile(mobileNumber);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Enter a valid Nepal mobile number (e.g. +97798XXXXXXXX).");
        }

        // Try to find the OTP record by mobile (stored in the email column)
        PasswordResetOtp record = passwordResetOtpRepository.findLatestByEmail(mobile)
                .orElseGet(() -> {
                    // Try without +977 prefix
                    String localNum = mobile.replace("+977", "");
                    return passwordResetOtpRepository.findLatestByEmail(localNum).orElse(null);
                });

        if (record == null) {
            throw new IllegalArgumentException(
                    "No verification code was requested. Please request a new code.");
        }

        if (record.isVerified() && record.getUsedAt() != null) {
            throw new IllegalArgumentException("This verification code has already been used. Please request a new one.");
        }

        if (record.getVerificationAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
            throw new IllegalArgumentException("Too many incorrect attempts. Please request a new code.");
        }

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("The verification code has expired. Please request a new code.");
        }

        record.setVerificationAttempts(record.getVerificationAttempts() + 1);

        if (!passwordEncoder.matches(otp.trim(), record.getOtpHash())) {
            passwordResetOtpRepository.save(record);
            throw new IllegalArgumentException("Incorrect verification code. Please try again.");
        }

        // OTP is valid
        record.setVerified(true);
        record.setUsedAt(LocalDateTime.now());
        passwordResetOtpRepository.save(record);

        // Find the user by phone number
        String encodedPassword = passwordEncoder.encode(newPassword);
        boolean updated = false;

        User user = userRepository.findByPhoneNumber(mobile).orElse(null);
        if (user == null) {
            user = userRepository.findByPhoneNumber(mobile.replace("+977", "")).orElse(null);
        }
        if (user != null) {
            user.setPassword(encodedPassword);
            userRepository.save(user);
            updated = true;
        }

        ShopOwner shopOwner = shopOwnerRepository.findByPhone(mobile).orElse(null);
        if (shopOwner == null) {
            shopOwner = shopOwnerRepository.findByPhone(mobile.replace("+977", "")).orElse(null);
        }
        if (shopOwner != null) {
            shopOwner.setPassword(encodedPassword);
            shopOwner.setPasswordChanged(true);
            shopOwnerRepository.save(shopOwner);
            updated = true;
        }

        if (!updated) {
            throw new IllegalArgumentException("No account found for this mobile number.");
        }

        log.info("Password reset completed for mobile {}", maskMobile(mobile));
        return "Your password has been reset successfully. You can now log in with your new password.";
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String generateOtp() {
        int max = (int) Math.pow(10, OTP_LENGTH);
        int min = max / 10;
        int value = SECURE_RANDOM.nextInt(max - min) + min;
        return String.valueOf(value);
    }

    private boolean isDevProfile() {
        return activeProfile != null && activeProfile.contains("dev");
    }

    static String normalizeMobile(String raw) {
        if (raw == null) throw new IllegalArgumentException("Mobile number is required");
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("977")) digits = digits.substring(3);
        if (digits.length() == 10 && (digits.startsWith("98")
                || digits.startsWith("97") || digits.startsWith("96"))) {
            return "+977" + digits;
        }
        throw new IllegalArgumentException(
                "Enter a valid Nepal mobile number (e.g. +97798XXXXXXXX).");
    }

    private static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) return "***";
        return mobile.substring(0, 5) + "****" + mobile.substring(mobile.length() - 3);
    }
}
