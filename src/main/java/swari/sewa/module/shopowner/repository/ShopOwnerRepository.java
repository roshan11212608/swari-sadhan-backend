package swari.sewa.module.shopowner.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.shopowner.model.ShopOwner;

import java.util.Optional;

@Repository
public interface ShopOwnerRepository extends JpaRepository<ShopOwner, Long> {
    
    Optional<ShopOwner> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
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
}
