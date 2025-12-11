package swari.sewa.module.publicuser.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import swari.sewa.common.enums.UserRole;
import swari.sewa.module.publicuser.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    java.util.List<User> findByRoleAndIsActive(@Param("role") UserRole role);
    
    java.util.List<User> findByRole(UserRole role);
    
    org.springframework.data.domain.Page<User> findByRole(UserRole role, org.springframework.data.domain.Pageable pageable);
    
    long countByRole(UserRole role);
    
    @Query("SELECT u FROM User u WHERE u.isActive = false")
    java.util.List<User> findInactiveUsers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.isActive = true")
    Long countByRoleAndIsActive(@Param("role") UserRole role);
}
