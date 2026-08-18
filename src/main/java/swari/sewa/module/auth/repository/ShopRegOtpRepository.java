package swari.sewa.module.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.auth.entity.ShopRegOtp;

import java.util.Optional;

@Repository
public interface ShopRegOtpRepository extends JpaRepository<ShopRegOtp, Long> {

    @Query("SELECT o FROM ShopRegOtp o WHERE o.email = :email AND o.mobileNumber = :mobile " +
           "ORDER BY o.createdAt DESC LIMIT 1")
    Optional<ShopRegOtp> findLatestByEmailAndMobile(@Param("email") String email,
                                                     @Param("mobile") String mobile);
}
