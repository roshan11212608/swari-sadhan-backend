package swari.sewa.module.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.user.entity.ShopOwner;

import java.util.Optional;

@Repository
public interface ShopOwnerRepository extends JpaRepository<ShopOwner, Long> {
    
    Optional<ShopOwner> findByEmail(String email);

    Optional<ShopOwner> findByPhone(String phone);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhone(String phone);
    
    boolean existsByLicenseNumber(String licenseNumber);
    
    Page<ShopOwner> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email, Pageable pageable);
    
    Page<ShopOwner> findByActive(Boolean active, Pageable pageable);
    
    long countByActive(Boolean active);
    
    @Query("SELECT COUNT(s) FROM ShopOwner s WHERE s.subscriptionActive = true AND s.subscriptionExpiresAt > CURRENT_TIMESTAMP")
    long countActiveSubscriptions();
    
    @Query("SELECT COUNT(s) FROM ShopOwner s WHERE s.kycVerified = true")
    long countVerifiedKYC();
    
    @Query("SELECT COUNT(s) FROM ShopOwner s WHERE s.emailVerified = true")
    long countVerifiedEmail();

    @Query("SELECT s FROM ShopOwner s WHERE NOT EXISTS (SELECT 1 FROM Subscription sub WHERE sub.shopOwnerId = s.id)")
    Page<ShopOwner> findUnsubscribed(Pageable pageable);
}
