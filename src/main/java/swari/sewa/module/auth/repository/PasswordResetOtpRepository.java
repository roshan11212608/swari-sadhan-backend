package swari.sewa.module.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.auth.entity.PasswordResetOtp;

import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    @Query("SELECT o FROM PasswordResetOtp o WHERE o.email = :email " +
           "ORDER BY o.createdAt DESC LIMIT 1")
    Optional<PasswordResetOtp> findLatestByEmail(@Param("email") String email);
}
