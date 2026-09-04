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

    /**
     * Lightweight projection: returns only the approval status and
     * password-changed flag for a shop owner, avoiding loading the full
     * entity (which has 40+ columns) when those are the only fields needed.
     * Used by the {@code emailExists} check in AuthService.
     */
    @Query("SELECT s.approvalStatus FROM ShopOwner s WHERE s.email = :email")
    Optional<String> findApprovalStatusByEmail(@Param("email") String email);
    
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

    // ── Dashboard credentials: filtered + paginated queries ──

    @Query("SELECT s FROM ShopOwner s WHERE " +
           "(LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "s.active = :active")
    Page<ShopOwner> searchByKeywordAndActive(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);

    @Query("SELECT s FROM ShopOwner s WHERE " +
           "(LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ShopOwner> searchByKeyword(@Param("search") String search, Pageable pageable);
}
