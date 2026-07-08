package swari.sewa.module.category.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import swari.sewa.module.category.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Category> findByIsActiveTrue();
    
    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.name")
    List<Category> findActiveCategoriesOrderByName();
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.category.id = :categoryId AND v.status = 'ACTIVE'")
    Long countActiveVehiclesByCategory(@Param("categoryId") Long categoryId);
}
