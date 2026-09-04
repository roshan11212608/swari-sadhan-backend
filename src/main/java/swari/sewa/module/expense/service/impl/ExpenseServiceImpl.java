package swari.sewa.module.expense.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.module.expense.dto.ExpenseDashboardResponse;
import swari.sewa.module.expense.dto.ExpenseAttachmentResponse;
import swari.sewa.module.expense.dto.ExpenseRequest;
import swari.sewa.module.expense.dto.ExpenseResponse;
import swari.sewa.module.expense.entity.Expense;
import swari.sewa.module.expense.entity.ExpenseAttachment;
import swari.sewa.module.expense.entity.ExpenseCategory;
import swari.sewa.module.expense.repository.ExpenseAttachmentRepository;
import swari.sewa.module.expense.repository.ExpenseCategoryRepository;
import swari.sewa.module.expense.repository.ExpenseRepository;
import swari.sewa.module.expense.service.ExpenseService;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ShopRepository shopRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ExpenseAttachmentRepository expenseAttachmentRepository;
    private final ModelMapper modelMapper;

    /** Resolve shop by user email, falling back to shop owner email.
     *  Handles race conditions where the shop was auto-created by the profile
     *  endpoint but the user link may not be set yet. */
    private Shop resolveShop(String userEmail) {
        return shopRepository.findByUserEmail(userEmail)
                .or(() -> shopRepository.findByShopOwnerEmail(userEmail))
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found for user: " + userEmail));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"financeDashboard", "financeIncome", "financeExpenses", "financeProfit", "financeCashFlow", "financeOutstanding", "vehicleInvestment", "paymentSummary", "analyticsDashboard", "analyticsStock", "analyticsPeriod", "analyticsProfitability", "analyticsAverage", "analyticsInventory"}, allEntries = true)
    public ExpenseResponse createExpense(ExpenseRequest expenseRequest, String userEmail) {
        Shop shop = resolveShop(userEmail);

        // Business validation
        validateExpenseRequest(expenseRequest);

        // Check for potential duplicates
        long duplicateCount = expenseRepository.countPotentialDuplicates(
                shop.getId(), 
                expenseRequest.getTitle(), 
                expenseRequest.getAmount(), 
                expenseRequest.getExpenseDate()
        );
        if (duplicateCount > 0) {
            log.warn("Potential duplicate expense detected for shop: {}, title: {}, amount: {}, date: {}", 
                    shop.getId(), expenseRequest.getTitle(), expenseRequest.getAmount(), expenseRequest.getExpenseDate());
        }

        // Generate thread-safe expense number
        String expenseNumber = expenseRepository.generateNextExpenseNumber();

        // Fetch category
        ExpenseCategory category = expenseCategoryRepository.findById(expenseRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + expenseRequest.getCategoryId()));

        Expense expense = modelMapper.map(expenseRequest, Expense.class);
        expense.setExpenseNumber(expenseNumber);
        expense.setShop(shop);
        expense.setCategory(category);
        expense.setCreatedBy(userEmail);
        expense.setIsActive(true);

        Expense savedExpense = expenseRepository.save(expense);
        log.info("Created expense: {} for shop: {}", expenseNumber, shop.getId());
        
        return mapToResponse(savedExpense);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExpenseResponse> getExpenseById(Long id, String userEmail) {
        Shop shop = resolveShop(userEmail);

        return expenseRepository.findByIdWithAttachments(id)
                .filter(expense -> expense.getShop().getId().equals(shop.getId()) && expense.getIsActive())
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExpenseResponse> getExpenseByNumber(String expenseNumber, String userEmail) {
        Shop shop = resolveShop(userEmail);

        return expenseRepository.findByExpenseNumber(expenseNumber)
                .filter(expense -> expense.getShop().getId().equals(shop.getId()) && expense.getIsActive())
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesByShop(Long shopId, Pageable pageable, String userEmail) {
        Shop shop = resolveShop(userEmail);

        if (!shop.getId().equals(shopId)) {
            throw new ResourceNotFoundException("Shop not found or access denied");
        }

        Page<Expense> expenses = expenseRepository.findByShopIdAndIsActiveTrueWithDetails(shopId, pageable);
        return expenses.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> searchExpenses(Long shopId, String searchTerm, Pageable pageable, String userEmail) {
        Shop shop = resolveShop(userEmail);

        if (!shop.getId().equals(shopId)) {
            throw new ResourceNotFoundException("Shop not found or access denied");
        }

        Page<Expense> expenses = expenseRepository.searchExpensesWithDetails(shopId, searchTerm, pageable);
        return expenses.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> filterExpenses(Long shopId, String title, Long categoryId, String paymentStatus,
                                                String paymentMethod, LocalDate startDate, LocalDate endDate,
                                                BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable, String userEmail) {
        Shop shop = resolveShop(userEmail);

        if (!shop.getId().equals(shopId)) {
            throw new ResourceNotFoundException("Shop not found or access denied");
        }

        Page<Expense> expenses = expenseRepository.findByFiltersWithDetails(
                shopId, title, categoryId, paymentStatus, paymentMethod,
                startDate, endDate, minAmount, maxAmount, pageable);
        return expenses.map(this::mapToResponse);
    }

    @Override
    @CacheEvict(value = {"financeDashboard", "financeIncome", "financeExpenses", "financeProfit", "financeCashFlow", "financeOutstanding", "vehicleInvestment", "paymentSummary", "analyticsDashboard", "analyticsStock", "analyticsPeriod", "analyticsProfitability", "analyticsAverage", "analyticsInventory"}, allEntries = true)
    public ExpenseResponse updateExpense(Long id, ExpenseRequest expenseRequest, String userEmail) {
        Shop shop = resolveShop(userEmail);

        // Business validation
        validateExpenseRequest(expenseRequest);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        if (!expense.getShop().getId().equals(shop.getId())) {
            throw new ResourceNotFoundException("Expense not found or access denied");
        }

        // Fetch category
        ExpenseCategory category = expenseCategoryRepository.findById(expenseRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + expenseRequest.getCategoryId()));

        modelMapper.map(expenseRequest, expense);
        expense.setCategory(category);
        expense.setUpdatedBy(userEmail);

        Expense updatedExpense = expenseRepository.save(expense);
        log.info("Updated expense: {} for shop: {}", id, shop.getId());
        
        return mapToResponse(updatedExpense);
    }

    @Override
    @CacheEvict(value = {"financeDashboard", "financeIncome", "financeExpenses", "financeProfit", "financeCashFlow", "financeOutstanding", "vehicleInvestment", "paymentSummary", "analyticsDashboard", "analyticsStock", "analyticsPeriod", "analyticsProfitability", "analyticsAverage", "analyticsInventory"}, allEntries = true)
    public void deleteExpense(Long id, String userEmail) {
        Shop shop = resolveShop(userEmail);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        if (!expense.getShop().getId().equals(shop.getId())) {
            throw new ResourceNotFoundException("Expense not found or access denied");
        }

        expense.setIsActive(false);
        expense.setUpdatedBy(userEmail);
        expenseRepository.save(expense);
        log.info("Soft deleted expense: {} for shop: {}", id, shop.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getRecentExpenses(Long shopId, int limit, String userEmail) {
        Shop shop = resolveShop(userEmail);

        if (!shop.getId().equals(shopId)) {
            throw new ResourceNotFoundException("Shop not found or access denied");
        }

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Expense> expenses = expenseRepository.findByShopIdAndIsActiveTrue(shopId, pageable);
        return expenses.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getUpcomingPayments(Long shopId, int limit, String userEmail) {
        Shop shop = resolveShop(userEmail);

        if (!shop.getId().equals(shopId)) {
            throw new ResourceNotFoundException("Shop not found or access denied");
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<Expense> expenses = expenseRepository.findUpcomingPaymentsWithDetails(shopId, pageable);
        return expenses.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTodayExpenses(Long shopId, String userEmail) {
        Shop shop = resolveShop(userEmail);

        if (!shop.getId().equals(shopId)) {
            throw new ResourceNotFoundException("Shop not found or access denied");
        }

        return expenseRepository.sumByShopIdAndExpenseDate(shopId, LocalDate.now()).orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getPendingPayments(Long shopId, String userEmail) {
        Shop shop = resolveShop(userEmail);

        if (!shop.getId().equals(shopId)) {
            throw new ResourceNotFoundException("Shop not found or access denied");
        }

        return expenseRepository.sumPendingByShopId(shopId).orElse(BigDecimal.ZERO);
    }

    private void validateExpenseRequest(ExpenseRequest expenseRequest) {
        // Business validation for payment status and amount combinations
        if (expenseRequest.getPaymentStatus() == swari.sewa.common.enums.ExpensePaymentStatus.PAID && 
            expenseRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Paid expenses must have an amount greater than 0");
        }
        
        if (expenseRequest.getPaymentStatus() == swari.sewa.common.enums.ExpensePaymentStatus.PENDING && 
            expenseRequest.getReferenceNumber() != null && !expenseRequest.getReferenceNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Reference number should not be provided for pending expenses");
        }
        
        if (expenseRequest.getPaymentStatus() == swari.sewa.common.enums.ExpensePaymentStatus.PAID && 
            (expenseRequest.getReferenceNumber() == null || expenseRequest.getReferenceNumber().trim().isEmpty())) {
            throw new IllegalArgumentException("Reference number is required for paid expenses");
        }
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        ExpenseResponse response = modelMapper.map(expense, ExpenseResponse.class);
        response.setShopId(expense.getShop().getId());
        
        if (expense.getCategory() != null) {
            response.setCategoryId(expense.getCategory().getId());
            response.setCategoryName(expense.getCategory().getName());
        }
        
        List<ExpenseAttachmentResponse> attachmentResponses = expense.getAttachments().stream()
                .map(attachment -> ExpenseAttachmentResponse.builder()
                        .id(attachment.getId())
                        .expenseId(attachment.getExpense().getId())
                        .fileName(attachment.getFileName())
                        .filePath(attachment.getFilePath())
                        .fileSize(attachment.getFileSize())
                        .fileType(attachment.getFileType())
                        .uploadedAt(attachment.getUploadedAt())
                        .build())
                .collect(Collectors.toList());
        response.setAttachments(attachmentResponses);
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseDashboardResponse getDashboardSummary(String userEmail, String period, LocalDate startDate, LocalDate endDate, Integer trendMonths) {
        Shop shop = resolveShop(userEmail);

        Long shopId = shop.getId();

        // Determine date range based on period
        LocalDate today = LocalDate.now();
        LocalDate periodStart;
        LocalDate periodEnd = today;
        String periodLabel;

        if (startDate != null && endDate != null) {
            // Custom range
            periodStart = startDate;
            periodEnd = endDate;
            periodLabel = "Custom Range";
        } else if (period != null) {
            switch (period.toLowerCase()) {
                case "today":
                    periodStart = today;
                    periodLabel = "Today";
                    break;
                case "thisweek":
                    periodStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
                    periodLabel = "This Week";
                    break;
                case "thismonth":
                    periodStart = today.with(TemporalAdjusters.firstDayOfMonth());
                    periodLabel = "This Month";
                    break;
                case "lastmonth":
                    periodStart = today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
                    periodEnd = today.with(TemporalAdjusters.firstDayOfMonth()).minusDays(1);
                    periodLabel = "Last Month";
                    break;
                case "thisyear":
                    periodStart = today.with(TemporalAdjusters.firstDayOfYear());
                    periodLabel = "This Year";
                    break;
                default:
                    periodStart = today.with(TemporalAdjusters.firstDayOfMonth());
                    periodLabel = "This Month";
            }
        } else {
            // Default to this month
            periodStart = today.with(TemporalAdjusters.firstDayOfMonth());
            periodLabel = "This Month";
        }

        // Summary Metrics
        BigDecimal periodExpense = expenseRepository.getExpenseByDateRange(shopId, periodStart, periodEnd);
        BigDecimal pendingPayments = expenseRepository.getPendingPayments(shopId);
        BigDecimal paidExpenses = expenseRepository.getPaidExpenses(shopId);
        Long totalExpenses = expenseRepository.getTotalExpenses(shopId);
        
        // Today, Yesterday, This Week expenses
        BigDecimal todayExpense = expenseRepository.getTodayExpense(shopId);
        BigDecimal yesterdayExpense = expenseRepository.getExpenseByDateRange(shopId, today.minusDays(1), today.minusDays(1));
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        BigDecimal weekExpense = expenseRepository.getExpenseByDateRange(shopId, weekStart, today);
        
        // Calculate average daily expense based on elapsed days in period
        long elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
        BigDecimal averageDailyExpense = elapsedDays > 0 
                ? periodExpense.divide(BigDecimal.valueOf(elapsedDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        ExpenseDashboardResponse.SummaryMetrics summary = ExpenseDashboardResponse.SummaryMetrics.builder()
                .periodExpense(periodExpense)
                .pendingPayments(pendingPayments)
                .paidExpenses(paidExpenses)
                .averageDailyExpense(averageDailyExpense)
                .totalExpenses(totalExpenses)
                .periodLabel(periodLabel)
                .todayExpense(todayExpense)
                .yesterdayExpense(yesterdayExpense)
                .weekExpense(weekExpense)
                .build();

        // Monthly Trend (configurable months)
        int months = trendMonths != null ? trendMonths : 12;
        List<Object[]> monthlyTrendData = expenseRepository.getMonthlyTrend(shopId, months);
        List<ExpenseDashboardResponse.MonthlyTrend> monthlyTrend = monthlyTrendData.stream()
                .map(row -> ExpenseDashboardResponse.MonthlyTrend.builder()
                        .month(getMonthName((Integer) row[0]))
                        .year(String.valueOf(row[1]))
                        .amount((BigDecimal) row[2])
                        .expenseCount(((Number) row[3]).longValue())
                        .build())
                .collect(Collectors.toList());

        // Monthly Comparison (this month vs last month)
        List<Object[]> comparisonResult = expenseRepository.getMonthlyComparison(shopId);
        BigDecimal currentPeriod = BigDecimal.ZERO;
        BigDecimal previousPeriod = BigDecimal.ZERO;
        if (comparisonResult != null && !comparisonResult.isEmpty()) {
            Object[] comparisonData = comparisonResult.get(0);
            currentPeriod = comparisonData[0] != null ? (BigDecimal) comparisonData[0] : BigDecimal.ZERO;
            previousPeriod = comparisonData[1] != null ? (BigDecimal) comparisonData[1] : BigDecimal.ZERO;
        }
        BigDecimal difference = currentPeriod.subtract(previousPeriod);
        BigDecimal percentageChange = previousPeriod.compareTo(BigDecimal.ZERO) > 0 
                ? difference.divide(previousPeriod, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        String trend = difference.compareTo(BigDecimal.ZERO) > 0 ? "UP" : 
                      difference.compareTo(BigDecimal.ZERO) < 0 ? "DOWN" : "SAME";

        ExpenseDashboardResponse.MonthlyComparison monthlyComparison = ExpenseDashboardResponse.MonthlyComparison.builder()
                .currentPeriod(currentPeriod)
                .previousPeriod(previousPeriod)
                .difference(difference)
                .percentageChange(percentageChange)
                .trend(trend)
                .build();

        // Payment Method Distribution
        List<Object[]> paymentMethodData = expenseRepository.getPaymentMethodDistribution(shopId);
        BigDecimal totalAmount = paymentMethodData.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<ExpenseDashboardResponse.PaymentMethodDistribution> paymentMethodDistribution = paymentMethodData.stream()
                .map(row -> {
                    BigDecimal amount = (BigDecimal) row[1];
                    Long count = ((Number) row[2]).longValue();
                    BigDecimal percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0 
                            ? amount.divide(totalAmount, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;
                    return ExpenseDashboardResponse.PaymentMethodDistribution.builder()
                            .paymentMethod(formatPaymentMethod(row[0].toString()))
                            .amount(amount)
                            .percentage(percentage)
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());

        // Recent Expenses
        List<ExpenseResponse> recentExpenses = getRecentExpenses(shopId, 5, userEmail);

        // Upcoming Payments
        List<ExpenseResponse> upcomingPayments = getUpcomingPayments(shopId, 3, userEmail);

        return ExpenseDashboardResponse.builder()
                .summary(summary)
                .monthlyTrend(monthlyTrend)
                .monthlyComparison(monthlyComparison)
                .paymentMethodDistribution(paymentMethodDistribution)
                .recentExpenses(recentExpenses)
                .upcomingPayments(upcomingPayments)
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"financeDashboard", "financeIncome", "financeExpenses", "financeProfit", "financeCashFlow", "financeOutstanding", "vehicleInvestment", "paymentSummary", "analyticsDashboard", "analyticsStock", "analyticsPeriod", "analyticsProfitability", "analyticsAverage", "analyticsInventory"}, allEntries = true)
    public void addAttachment(Long expenseId, String fileName, String filePath, Long fileSize, String fileType, String userEmail) {
        log.info("=== ADD ATTACHMENT CALLED ===");
        log.info("Expense ID: {}", expenseId);
        log.info("File Name: {}", fileName);
        log.info("File Path: {}", filePath);
        log.info("File Size: {}", fileSize);
        log.info("File Type: {}", fileType);
        log.info("User Email: {}", userEmail);
        
        Shop shop = resolveShop(userEmail);

        log.info("Shop ID: {}", shop.getId());

        Expense expense = expenseRepository.findById(expenseId)
                .filter(e -> e.getShop().getId().equals(shop.getId()) && e.getIsActive())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));

        log.info("Expense found: {}", expense.getExpenseNumber());
        log.info("Current attachments count: {}", expense.getAttachments().size());

        ExpenseAttachment attachment = ExpenseAttachment.builder()
                .expense(expense)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(fileSize)
                .fileType(fileType)
                .build();

        ExpenseAttachment savedAttachment = expenseAttachmentRepository.save(attachment);
        log.info("Attachment saved with ID: {}", savedAttachment.getId());
        
        // Add to expense's attachments set to ensure relationship is established
        expense.getAttachments().add(savedAttachment);
        expenseRepository.save(expense);
        log.info("Expense saved with attachments count: {}", expense.getAttachments().size());
        
        log.info("Added attachment {} to expense {} with ID {}", fileName, expenseId, savedAttachment.getId());
    }

    private String formatPaymentMethod(String enumValue) {
        return enumValue.replace("_", " ");
    }

    private String getMonthName(int month) {
        String[] months = {"January", "February", "March", "April", "May", "June", 
                          "July", "August", "September", "October", "November", "December"};
        return months[month - 1];
    }
}
