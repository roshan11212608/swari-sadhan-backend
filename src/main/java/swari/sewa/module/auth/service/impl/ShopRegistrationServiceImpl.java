package swari.sewa.module.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.auth.dto.ShopRegSendOtpRequest;
import swari.sewa.module.auth.dto.ShopRegVerifyOtpRequest;
import swari.sewa.module.auth.dto.ShopRegVerifyOtpResponse;
import swari.sewa.module.auth.entity.ShopRegOtp;
import swari.sewa.module.auth.repository.ShopRegOtpRepository;
import swari.sewa.module.auth.service.BrevoSmsService;
import swari.sewa.module.auth.service.EmailService;
import swari.sewa.module.auth.service.ShopRegistrationService;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ShopRegistrationServiceImpl implements ShopRegistrationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MINUTES = 5;
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int TOKEN_TTL_MINUTES = 30;

    private final ShopRegOtpRepository shopRegOtpRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final UserRepository userRepository;
    private final BrevoSmsService brevoSmsService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String sendOtps(ShopRegSendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String mobile;
        try {
            mobile = normalizeMobile(request.getMobileNumber());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Enter a valid Nepal mobile number (e.g. +97798XXXXXXXX).");
        }

        // Prevent duplicate registrations, but allow a REJECTED shop owner to
        // re-apply using the same email/mobile. A REJECTED application is reused
        // (not duplicated) in createShopOwner, so here we only need to let the
        // OTP through.
        Optional<ShopOwner> existingOwner = shopOwnerRepository.findByEmail(email);
        if (existingOwner.isPresent()) {
            String status = existingOwner.get().getApprovalStatus();
            if ("PENDING".equals(status)) {
                throw new IllegalArgumentException("Your application is already pending admin approval.");
            }
            if ("APPROVED".equals(status)) {
                throw new IllegalArgumentException("This email is already registered. Kindly login.");
            }
            // REJECTED → allowed to re-apply; fall through.
        } else if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("This email is already registered. Kindly login.");
        }

        // Same status-aware check for the mobile number.
        Optional<ShopOwner> existingByPhone = shopOwnerRepository.findByPhone(mobile);
        if (existingByPhone.isPresent()) {
            String status = existingByPhone.get().getApprovalStatus();
            if ("PENDING".equals(status)) {
                throw new IllegalArgumentException("Your application is already pending admin approval.");
            }
            if ("APPROVED".equals(status)) {
                throw new IllegalArgumentException("This mobile number is already registered. Kindly login.");
            }
            // REJECTED → allowed to re-apply; fall through.
        } else if (userRepository.existsByPhoneNumber(mobile)) {
            throw new IllegalArgumentException("This mobile number is already registered. Kindly login.");
        }

        // Rate limiting: check last sent time
        shopRegOtpRepository.findLatestByEmailAndMobile(email, mobile)
                .ifPresent(existing -> {
                    if (existing.getLastSentAt() != null
                            && existing.getLastSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS)
                            .isAfter(LocalDateTime.now())) {
                        throw new IllegalArgumentException("Please wait before requesting another OTP.");
                    }
                });

        String emailOtp = generateOtp();

        ShopRegOtp record = ShopRegOtp.builder()
                .email(email)
                .mobileNumber(mobile)
                .emailOtpHash(passwordEncoder.encode(emailOtp))
                .mobileOtpHash(passwordEncoder.encode("MOBILE_OTP_NOT_REQUIRED"))
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .verificationAttempts(0)
                .resendCount(0)
                .lastSentAt(LocalDateTime.now())
                .verified(false)
                .build();

        shopRegOtpRepository.save(record);

        // Send email OTP
        String emailSubject = "Swari Sadhan - Your Shop Registration Verification Code";
        String emailBody = "<div style='font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:20px'>"
                + "<h2 style='color:#f97316'>Swari Sadhan</h2>"
                + "<p>Your shop registration verification code is:</p>"
                + "<h1 style='font-size:32px;letter-spacing:5px;color:#f97316'>" + emailOtp + "</h1>"
                + "<p>This code is valid for " + OTP_TTL_MINUTES + " minutes. Do not share it.</p>"
                + "</div>";
        emailService.sendHtmlEmail(email, emailSubject, emailBody);

        log.info("Shop registration email OTP sent for email {} / mobile {}", email, maskMobile(mobile));
        return "Verification code sent to your email.";
    }

    @Override
    public ShopRegVerifyOtpResponse verifyOtps(ShopRegVerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String mobile;
        try {
            mobile = normalizeMobile(request.getMobileNumber());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Enter a valid Nepal mobile number (e.g. +97798XXXXXXXX).");
        }

        ShopRegOtp record = shopRegOtpRepository.findLatestByEmailAndMobile(email, mobile)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No OTP was requested for this email/mobile. Please request new codes."));

        if (record.isVerified() && record.getUsedAt() != null) {
            throw new IllegalArgumentException("This verification has already been used. Please start over.");
        }

        if (record.getVerificationAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
            throw new IllegalArgumentException("Too many incorrect attempts. Please request new codes.");
        }

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("The verification codes have expired. Please request new codes.");
        }

        record.setVerificationAttempts(record.getVerificationAttempts() + 1);

        boolean emailMatch = passwordEncoder.matches(request.getEmailOtp(), record.getEmailOtpHash());

        if (!emailMatch) {
            shopRegOtpRepository.save(record);
            throw new IllegalArgumentException("Incorrect email code. Please try again.");
        }

        // Both OTPs match — issue a verification token
        String rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        record.setVerified(true);
        record.setVerificationTokenHash(hashSha256(rawToken));
        record.setTokenExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES));
        shopRegOtpRepository.save(record);

        log.info("Shop registration email verified for email {} / mobile {}", email, maskMobile(mobile));

        return ShopRegVerifyOtpResponse.builder()
                .signupVerificationToken(rawToken)
                .message("Email verified successfully.")
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

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

    private String generateOtp() {
        int max = (int) Math.pow(10, OTP_LENGTH);
        int min = max / 10;
        int value = SECURE_RANDOM.nextInt(max - min) + min;
        return String.valueOf(value);
    }

    static String maskMobile(String mobile) {
        String digits = mobile.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return "****";
        return "+977 " + digits.substring(0, 2) + "******" + digits.substring(digits.length() - 2);
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
