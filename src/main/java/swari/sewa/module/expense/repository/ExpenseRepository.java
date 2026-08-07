package swari.sewa.module.expense.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swari.sewa.module.expense.entity.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.attachments WHERE e.id = :id")
    Optional<Expense> findByIdWithAttachments(@Param("id") Long id);
    
    Optional<Expense> findByExpenseNumber(String expenseNumber);
    
    boolean existsByExpenseNumber(String expenseNumber);
    
    @Query("SELECT COUNT(e) FROM Expense e WHERE e.shop.id = :shopId AND e.isActive = true " +
           "AND e.title = :title AND e.amount = :amount AND e.expenseDate = :expenseDate")
    long countPotentialDuplicates(@Param("shopId") Long shopId, @Param("title") String title, 
                                  @Param("amount") BigDecimal amount, @Param("expenseDate") LocalDate expenseDate);
    
    List<Expense> findByShopIdAndIsActiveTrue(Long shopId);
    
    Page<Expense> findByShopIdAndIsActiveTrue(Long shopId, Pageable pageable);
    
    @Query("SELECT e FROM Expense e WHERE e.shop.id = :shopId AND e.isActive = true " +
           "AND (:title IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:categoryId IS NULL OR e.category.id = :categoryId) " +
           "AND (:paymentStatus IS NULL OR e.paymentStatus = :paymentStatus) " +
           "AND (:paymentMethod IS NULL OR e.paymentMethod = :paymentMethod) " +
           "AND (:startDate IS NULL OR e.expenseDate >= :startDate) " +
           "AND (:endDate IS NULL OR e.expenseDate <= :endDate) " +
           "AND (:minAmount IS NULL OR e.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR e.amount <= :maxAmount)")
    Page<Expense> findByFilters(
        @Param("shopId") Long shopId,
        @Param("title") String title,
        @Param("categoryId") Long categoryId,
        @Param("paymentStatus") String paymentStatus,
        @Param("paymentMethod") String paymentMethod,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("minAmount") BigDecimal minAmount,
        @Param("maxAmount") BigDecimal maxAmount,
        Pageable pageable
    );
    
    @Query("SELECT e FROM Expense e WHERE e.shop.id = :shopId AND e.isActive = true " +
           "AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(e.category.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(e.vendorPaidTo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(e.referenceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Expense> searchExpenses(@Param("shopId") Long shopId, @Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.shop.id = :shopId AND e.isActive = true AND e.expenseDate = :date")
    Optional<BigDecimal> sumByShopIdAndExpenseDate(@Param("shopId") Long shopId, @Param("date") LocalDate date);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.shop.id = :shopId AND e.isActive = true " +
           "AND e.expenseDate BETWEEN :startDate AND :endDate")
    Optional<BigDecimal> sumByShopIdAndDateRange(@Param("shopId") Long shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.shop.id = :shopId AND e.isActive = true AND e.paymentStatus = 'PENDING'")
    Optional<BigDecimal> sumPendingByShopId(@Param("shopId") Long shopId);
    
    @Query("SELECT e FROM Expense e WHERE e.shop.id = :shopId AND e.isActive = true AND e.paymentStatus IN ('PENDING', 'PARTIALLY_PAID') " +
           "ORDER BY e.dueDate ASC NULLS LAST")
    List<Expense> findUpcomingPayments(@Param("shopId") Long shopId, Pageable pageable);
    
    @Query(value = "SELECT CONCAT('EXP-', YEAR(CURDATE()), '-', LPAD(COALESCE(MAX(CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(expense_number, '-', -1), '-', 1) AS UNSIGNED)), 0) + 1, 6, '0')) FROM expenses WHERE expense_number LIKE CONCAT('EXP-', YEAR(CURDATE()), '-%') FOR UPDATE", nativeQuery = true)
    String generateNextExpenseNumber();
    
    // Dashboard Analytics Queries
    
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.shop.id = :shopId AND e.isActive = true AND e.expenseDate = CURRENT_DATE")
    BigDecimal getTodayExpense(@Param("shopId") Long shopId);
    
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.shop.id = :shopId AND e.isActive = true AND e.expenseDate >= :startDate AND e.expenseDate <= :endDate")
    BigDecimal getExpenseByDateRange(@Param("shopId") Long shopId, @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.shop.id = :shopId AND e.isActive = true AND e.paymentStatus = 'PENDING'")
    BigDecimal getPendingPayments(@Param("shopId") Long shopId);
    
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.shop.id = :shopId AND e.isActive = true AND e.paymentStatus = 'PAID'")
    BigDecimal getPaidExpenses(@Param("shopId") Long shopId);
    
    @Query("SELECT COUNT(e) FROM Expense e WHERE e.shop.id = :shopId AND e.isActive = true")
    Long getTotalExpenses(@Param("shopId") Long shopId);
    
    @Query(value = "SELECT MONTH(expense_date) as month, YEAR(expense_date) as year, SUM(amount) as amount, COUNT(*) as count " +
           "FROM expenses WHERE shop_id = :shopId AND is_active = true " +
           "AND expense_date >= DATE_SUB(CURDATE(), INTERVAL :months MONTH) " +
           "GROUP BY YEAR(expense_date), MONTH(expense_date) " +
           "ORDER BY YEAR(expense_date), MONTH(expense_date)", nativeQuery = true)
    List<Object[]> getMonthlyTrend(@Param("shopId") Long shopId, @Param("months") int months);
    
    @Query(value = "SELECT " +
           "COALESCE(SUM(CASE WHEN MONTH(expense_date) = MONTH(CURDATE()) AND YEAR(expense_date) = YEAR(CURDATE()) THEN amount ELSE 0 END), 0) as thisMonth, " +
           "COALESCE(SUM(CASE WHEN MONTH(expense_date) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) AND YEAR(expense_date) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) THEN amount ELSE 0 END), 0) as lastMonth " +
           "FROM expenses WHERE shop_id = :shopId AND is_active = true", nativeQuery = true)
    List<Object[]> getMonthlyComparison(@Param("shopId") Long shopId);
    
    @Query("SELECT e.paymentMethod, SUM(e.amount), COUNT(e) FROM Expense e " +
           "WHERE e.shop.id = :shopId AND e.isActive = true " +
           "GROUP BY e.paymentMethod")
    List<Object[]> getPaymentMethodDistribution(@Param("shopId") Long shopId);
    
    // Analytics Queries
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.shop.id = :shopId AND e.isActive = true AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByShopIdAndExpenseDateBetween(@Param("shopId") Long shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query(value = "SELECT " +
           "period, " +
           "COALESCE(SUM(expenses), 0) as expenses, " +
           "0 as purchases " +
           "FROM (" +
           "  SELECT " +
           "  CASE WHEN :isYearly = true THEN CAST(YEAR(e.expense_date) AS CHAR) ELSE SUBSTRING(MONTHNAME(e.expense_date), 1, 3) END as period, " +
           "  e.amount as expenses " +
           "  FROM expenses e " +
           "  WHERE e.shop_id = :shopId AND e.expense_date BETWEEN :startDate AND :endDate AND e.is_active = true " +
           ") as subq " +
           "GROUP BY period " +
           "ORDER BY CASE WHEN :isYearly = true THEN period ELSE " +
           "CASE period WHEN 'Jan' THEN 1 WHEN 'Feb' THEN 2 WHEN 'Mar' THEN 3 WHEN 'Apr' THEN 4 " +
           "WHEN 'May' THEN 5 WHEN 'Jun' THEN 6 WHEN 'Jul' THEN 7 WHEN 'Aug' THEN 8 " +
           "WHEN 'Sep' THEN 9 WHEN 'Oct' THEN 10 WHEN 'Nov' THEN 11 WHEN 'Dec' THEN 12 END END", nativeQuery = true)
    List<Object[]> getExpenseTrend(@Param("shopId") Long shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("isYearly") Boolean isYearly);
    
    @Query(value = "SELECT ec.name as name, SUM(e.amount) as value, ec.color as color " +
           "FROM expenses e JOIN expense_categories ec ON e.category_id = ec.id " +
           "WHERE e.shop_id = :shopId AND e.expense_date BETWEEN :startDate AND :endDate AND e.is_active = true AND ec.is_active = true " +
           "GROUP BY ec.id, ec.name, ec.color", nativeQuery = true)
    List<Object[]> getExpenseCategories(@Param("shopId") Long shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
