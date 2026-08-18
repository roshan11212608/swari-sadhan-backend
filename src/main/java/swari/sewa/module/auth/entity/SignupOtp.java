package swari.sewa.module.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persistent record for the mobile-number OTP signup flow.
 * <p>
 * Security notes:
 * <ul>
 *   <li>Only a BCrypt hash of the OTP is stored ({@code otpHash}). The raw OTP
 *       is never persisted or logged.</li>
 *   <li>After successful verification a single-use verification token
 *       (also hashed) is stored so the backend can prove the mobile number
 *       was verified when the account is finally created.</li>
 *   <li>A record is marked consumed via {@code usedAt} once the signup
 *       completes, preventing token reuse.</li>
 * </ul>
 */
@Entity
@Table(name = "signup_otp")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    /** BCrypt hash of the 6-digit OTP. */
    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verification_attempts", nullable = false)
    private int verificationAttempts;

    @Column(name = "resend_count", nullable = false)
    private int resendCount;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    /** SHA-256 hash of the verification token issued after a successful OTP. */
    @Column(name = "verification_token_hash")
    private String verificationTokenHash;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    /** When the verification token was consumed by a successful signup. */
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
