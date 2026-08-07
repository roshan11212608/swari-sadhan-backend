package swari.sewa.module.employee.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import swari.sewa.module.employee.entity.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    @Query("SELECT e FROM Employee e WHERE e.employeeNumber = :employeeNumber AND e.deletedAt IS NULL")
    Optional<Employee> findByEmployeeNumber(@Param("employeeNumber") String employeeNumber);

    @Query("SELECT e FROM Employee e WHERE e.mobileNumber = :mobileNumber AND e.deletedAt IS NULL")
    List<Employee> findByMobileNumber(@Param("mobileNumber") String mobileNumber);

    List<Employee> findByShopId(Long shopId);
    
    Page<Employee> findByShopId(Long shopId, Pageable pageable);
    
    Page<Employee> findByShopIdAndStatus(Long shopId, String status, Pageable pageable);
    
    Page<Employee> findByShopIdAndDepartment(Long shopId, String department, Pageable pageable);
    
    Page<Employee> findByShopIdAndEmploymentType(Long shopId, String employmentType, Pageable pageable);
    
    @Query("SELECT e FROM Employee e WHERE e.shop.id = :shopId AND e.status = :status AND e.department = :department AND e.deletedAt IS NULL")
    Page<Employee> findByShopIdAndStatusAndDepartment(
        @Param("shopId") Long shopId,
        @Param("status") String status,
        @Param("department") String department,
        Pageable pageable
    );
    
    @Query("SELECT e FROM Employee e WHERE e.shop.id = :shopId AND e.status = :status AND e.employmentType = :employmentType AND e.deletedAt IS NULL")
    Page<Employee> findByShopIdAndStatusAndEmploymentType(
        @Param("shopId") Long shopId,
        @Param("status") String status,
        @Param("employmentType") String employmentType,
        Pageable pageable
    );
    
    @Query("SELECT e FROM Employee e WHERE e.shop.id = :shopId AND e.department = :department AND e.employmentType = :employmentType AND e.deletedAt IS NULL")
    Page<Employee> findByShopIdAndDepartmentAndEmploymentType(
        @Param("shopId") Long shopId,
        @Param("department") String department,
        @Param("employmentType") String employmentType,
        Pageable pageable
    );
    
    @Query("SELECT e FROM Employee e WHERE e.shop.id = :shopId AND e.status = :status AND e.department = :department AND e.employmentType = :employmentType AND e.deletedAt IS NULL")
    Page<Employee> findByShopIdAndStatusAndDepartmentAndEmploymentType(
        @Param("shopId") Long shopId,
        @Param("status") String status,
        @Param("department") String department,
        @Param("employmentType") String employmentType,
        Pageable pageable
    );
    
    @Query("SELECT e FROM Employee e WHERE e.shop.id = :shopId AND " +
           "(e.fullName LIKE %:search% OR e.employeeNumber LIKE %:search% OR " +
           "e.mobileNumber LIKE %:search%) AND e.deletedAt IS NULL")
    Page<Employee> searchEmployees(@Param("shopId") Long shopId, @Param("search") String search, Pageable pageable);
    
    @Query("SELECT e FROM Employee e WHERE e.shop.id = :shopId AND e.deletedAt IS NULL")
    List<Employee> findActiveByShopId(@Param("shopId") Long shopId);
    
    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.shop WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Employee> findByIdWithShop(@Param("id") Long id);
    
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.shop.id = :shopId AND e.status = :status AND e.deletedAt IS NULL")
    Long countByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") String status);
    
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.shop.id = :shopId AND e.deletedAt IS NULL")
    Long countActiveByShopId(@Param("shopId") Long shopId);
    
    Optional<Employee> findTopByShopIdOrderByEmployeeNumberDesc(Long shopId);
}
