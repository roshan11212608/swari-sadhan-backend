package swari.sewa.module.expense.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swari.sewa.module.expense.dto.ExpenseDashboardResponse;
import swari.sewa.module.expense.dto.ExpenseRequest;
import swari.sewa.module.expense.dto.ExpenseResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseService {
    
    ExpenseResponse createExpense(ExpenseRequest expenseRequest, String userEmail);
    
    Optional<ExpenseResponse> getExpenseById(Long id, String userEmail);
    
    Optional<ExpenseResponse> getExpenseByNumber(String expenseNumber, String userEmail);
    
    Page<ExpenseResponse> getExpensesByShop(Long shopId, Pageable pageable, String userEmail);
    
    Page<ExpenseResponse> searchExpenses(Long shopId, String searchTerm, Pageable pageable, String userEmail);
    
    Page<ExpenseResponse> filterExpenses(Long shopId, String title, Long categoryId, String paymentStatus, 
                                        String paymentMethod, LocalDate startDate, LocalDate endDate,
                                        BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable, String userEmail);
    
    ExpenseResponse updateExpense(Long id, ExpenseRequest expenseRequest, String userEmail);
    
    void deleteExpense(Long id, String userEmail);
    
    List<ExpenseResponse> getRecentExpenses(Long shopId, int limit, String userEmail);
    
    List<ExpenseResponse> getUpcomingPayments(Long shopId, int limit, String userEmail);
    
    BigDecimal getTodayExpenses(Long shopId, String userEmail);
    
    BigDecimal getPendingPayments(Long shopId, String userEmail);
    
    ExpenseDashboardResponse getDashboardSummary(String userEmail, String period, LocalDate startDate, LocalDate endDate, Integer trendMonths);
    
    void addAttachment(Long expenseId, String fileName, String filePath, Long fileSize, String fileType, String userEmail);
}
