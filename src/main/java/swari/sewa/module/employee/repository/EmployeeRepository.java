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

    // ── Combined search + filter + pagination (replaces /all + client-side filtering) ──

    @Query("SELECT e FROM Employee e WHERE e.shop.id = :shopId AND e.deletedAt IS NULL " +
           "AND (:search IS NULL OR e.fullName LIKE %:search% OR e.employeeNumber LIKE %:search% OR e.mobileNumber LIKE %:search%) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:department IS NULL OR e.department = :department) " +
           "AND (:employmentType IS NULL OR e.employmentType = :employmentType)")
    Page<Employee> findByShopIdWithSearchAndFilters(
            @Param("shopId") Long shopId,
            @Param("search") String search,
            @Param("status") String status,
            @Param("department") String department,
            @Param("employmentType") String employmentType,
            Pageable pageable);

    // ── Paginated query ordered by joining date desc (for recent-employees dashboard) ──

    @Query("SELECT e FROM Employee e WHERE e.shop.id = :shopId AND e.deletedAt IS NULL ORDER BY e.joiningDate DESC")
    Page<Employee> findByShopIdOrderByJoiningDateDesc(@Param("shopId") Long shopId, Pageable pageable);

    // ── Salary distribution by department (replaces loading ALL employees) ──

    @Query("SELECT e.department, SUM(e.basicSalary) FROM Employee e " +
           "WHERE e.shop.id = :shopId AND e.deletedAt IS NULL GROUP BY e.department")
    List<Object[]> sumBasicSalaryGroupByDepartment(@Param("shopId") Long shopId);

    // ── Filter options (replaces deriving from ALL employees in JS) ──

    @Query("SELECT DISTINCT e.department FROM Employee e WHERE e.shop.id = :shopId AND e.deletedAt IS NULL AND e.department IS NOT NULL")
    List<String> findDistinctDepartmentsByShopId(@Param("shopId") Long shopId);

    @Query("SELECT DISTINCT e.employmentType FROM Employee e WHERE e.shop.id = :shopId AND e.deletedAt IS NULL AND e.employmentType IS NOT NULL")
    List<String> findDistinctEmploymentTypesByShopId(@Param("shopId") Long shopId);

    @Query("SELECT DISTINCT e.status FROM Employee e WHERE e.shop.id = :shopId AND e.deletedAt IS NULL AND e.status IS NOT NULL")
    List<String> findDistinctStatusesByShopId(@Param("shopId") Long shopId);
}
