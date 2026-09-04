package swari.sewa.module.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import swari.sewa.common.enums.UserRole;
import swari.sewa.module.user.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    java.util.List<User> findByRoleAndIsActive(@Param("role") UserRole role);
    
    java.util.List<User> findByRole(UserRole role);
    
    org.springframework.data.domain.Page<User> findByRole(UserRole role, org.springframework.data.domain.Pageable pageable);
    
    long countByRole(UserRole role);
    
    @Query("SELECT u FROM User u WHERE u.isActive = false")
    java.util.List<User> findInactiveUsers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.isActive = true")
    Long countByRoleAndIsActive(@Param("role") UserRole role);

    @Query("SELECT MAX(u.customerCode) FROM User u WHERE u.customerCode LIKE :prefix%")
    String findMaxCustomerCodeByPrefix(@Param("prefix") String prefix);

    // ── Dashboard credentials: filtered + paginated queries ──

    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
           "(LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<User> findByRoleAndSearch(@Param("role") UserRole role, @Param("search") String search, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = :active AND " +
           "(LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<User> findByRoleAndSearchAndActive(@Param("role") UserRole role, @Param("search") String search, @Param("active") Boolean active, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = :active")
    org.springframework.data.domain.Page<User> findByRoleAndActive(@Param("role") UserRole role, @Param("active") Boolean active, org.springframework.data.domain.Pageable pageable);
}
