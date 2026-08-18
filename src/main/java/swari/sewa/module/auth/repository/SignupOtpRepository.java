package swari.sewa.module.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.auth.entity.SignupOtp;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SignupOtpRepository extends JpaRepository<SignupOtp, Long> {

    /** Most recently created OTP record for a given mobile number. */
    Optional<SignupOtp> findTopByMobileNumberOrderByCreatedAtDesc(String mobileNumber);

    /**
     * A verified, unused record whose verification token is still valid.
     * Used by the complete-signup step to validate the verification token.
     */
    @Query("SELECT o FROM SignupOtp o WHERE o.mobileNumber = :mobile " +
           "AND o.verified = true AND o.usedAt IS NULL " +
           "AND o.verificationTokenHash IS NOT NULL " +
           "AND o.tokenExpiresAt > :now")
    Optional<SignupOtp> findValidVerified(@Param("mobile") String mobile, @Param("now") LocalDateTime now);

    /** Invalidate (mark used) all open verified records for a mobile number. */
    @Modifying
    @Query("UPDATE SignupOtp o SET o.usedAt = :now, o.updatedAt = :now " +
           "WHERE o.mobileNumber = :mobile AND o.usedAt IS NULL")
    int invalidateOpenRecords(@Param("mobile") String mobile, @Param("now") LocalDateTime now);
}
