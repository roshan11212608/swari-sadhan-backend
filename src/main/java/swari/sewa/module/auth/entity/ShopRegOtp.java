package swari.sewa.module.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OTP record for shop-owner registration. Stores hashed OTPs for both the
 * owner's email and mobile number, plus a single-use verification token
 * issued after both are verified.
 */
@Entity
@Table(name = "shop_reg_otp")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopRegOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    /** BCrypt hash of the 6-digit email OTP. */
    @Column(name = "email_otp_hash", nullable = false)
    private String emailOtpHash;

    /** BCrypt hash of the 6-digit mobile OTP. */
    @Column(name = "mobile_otp_hash", nullable = false)
    private String mobileOtpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verification_attempts", nullable = false)
    private int verificationAttempts;

    @Column(name = "resend_count", nullable = false)
    private int resendCount;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(nullable = false)
    private boolean verified;

    /** SHA-256 hash of the verification token issued after both OTPs match. */
    @Column(name = "verification_token_hash")
    private String verificationTokenHash;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
