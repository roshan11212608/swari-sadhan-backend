package swari.sewa.module.finance.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.analytics.engine.BusinessCalculationEngine;
import swari.sewa.module.analytics.util.DateFilterUtil;
import swari.sewa.module.expense.repository.ExpenseRepository;
import swari.sewa.module.finance.dto.*;
import swari.sewa.module.finance.service.FinanceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FinanceServiceImpl implements FinanceService {

    private final BusinessCalculationEngine businessCalculationEngine;
    private final ExpenseRepository expenseRepository;

    @Override
    @Transactional(readOnly = true)
    public FinancialDashboardResponse getFinancialDashboard(Long shopId, String filter) {
        DateFilterUtil.DateRange dateRange = DateFilterUtil.getDateRange(filter);
        boolean isYearly = filter.equalsIgnoreCase("thisyear");

        // Get KPIs using BusinessCalculationEngine (single source of truth)
        BigDecimal salesRevenue = businessCalculationEngine.getSalesValue(shopId, dateRange.getFrom(), dateRange.getTo());
        BigDecimal inventoryPurchase = businessCalculationEngine.getInventoryPurchased(shopId, dateRange.getFrom().toLocalDate(), dateRange.getTo().toLocalDate());
        BigDecimal operatingExpenses = businessCalculationEngine.getOperatingExpenses(shopId, dateRange.getFrom().toLocalDate(), dateRange.getTo().toLocalDate());
        BigDecimal grossProfit = businessCalculationEngine.getGrossProfit(shopId, dateRange.getFrom(), dateRange.getTo());
        BigDecimal netProfit = businessCalculationEngine.getNetProfit(shopId, dateRange.getFrom(), dateRange.getTo());
        BigDecimal profitMargin = businessCalculationEngine.getProfitMargin(shopId, dateRange.getFrom(), dateRange.getTo());
        
        // Cash available (simplified - can be enhanced with actual cash tracking)
        BigDecimal cashAvailable = netProfit;

        FinancialDashboardResponse.FinancialKPI kpi = FinancialDashboardResponse.FinancialKPI.builder()
                .salesRevenue(salesRevenue)
                .inventoryPurchase(inventoryPurchase)
                .operatingExpenses(operatingExpenses)
                .grossProfit(grossProfit)
                .netProfit(netProfit)
                .profitMargin(profitMargin)
                .cashAvailable(cashAvailable)
                .build();

        // Get trends from BusinessCalculationEngine
        List<BusinessCalculationEngine.RevenueTrendData> revenueTrend = 
            businessCalculationEngine.getRevenueTrend(shopId, dateRange.getFrom(), dateRange.getTo(), isYearly);
        List<BusinessCalculationEngine.ExpenseTrendData> expenseTrend = 
            businessCalculationEngine.getExpenseTrend(shopId, dateRange.getFrom().toLocalDate(), dateRange.getTo().toLocalDate(), isYearly);
        List<BusinessCalculationEngine.ProfitTrendData> profitTrend = 
            businessCalculationEngine.getProfitTrend(shopId, dateRange.getFrom(), dateRange.getTo(), isYearly);

        // Compose revenue vs expense trend from engine data
        List<FinancialDashboardResponse.RevenueVsExpenseData> revenueVsExpenseTrend = new ArrayList<>();
        for (int i = 0; i < revenueTrend.size(); i++) {
            BusinessCalculationEngine.RevenueTrendData rev = revenueTrend.get(i);
            BusinessCalculationEngine.ExpenseTrendData exp = i < expenseTrend.size() ? expenseTrend.get(i) : null;
            revenueVsExpenseTrend.add(FinancialDashboardResponse.RevenueVsExpenseData.builder()
                    .period(rev.getPeriod())
                    .revenue(rev.getRevenue())
                    .expenses(exp != null ? exp.getExpenses() : BigDecimal.ZERO)
                    .build());
        }

        // Compose profit trend from engine data
        List<FinancialDashboardResponse.ProfitTrendData> profitTrendList = profitTrend.stream()
            .map(pt -> FinancialDashboardResponse.ProfitTrendData.builder()
                    .period(pt.getPeriod())
                    .grossProfit(pt.getGrossProfit())
                    .build())
            .collect(Collectors.toList());

        // Get expense categories (still from repository as it's not in engine yet)
        List<Object[]> expenseCategoriesData = expenseRepository.getExpenseCategories(shopId, dateRange.getFrom().toLocalDate(), dateRange.getTo().toLocalDate());
        List<FinancialDashboardResponse.ExpenseCategoryData> expenseCategories = new ArrayList<>();
        for (Object[] row : expenseCategoriesData) {
            expenseCategories.add(FinancialDashboardResponse.ExpenseCategoryData.builder()
                    .name((String) row[0])
                    .value(row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(((Number) row[1]).longValue()))
                    .color((String) row[2])
                    .build());
        }

        // Generate yearly overview (12 months)
        List<FinancialDashboardResponse.YearlyOverviewData> yearlyOverview = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        
        for (int i = 11; i >= 0; i--) {
            LocalDate monthDate = today.minusMonths(i);
            LocalDate monthStart = monthDate.withDayOfMonth(1);
            LocalDate monthEnd = monthDate.withDayOfMonth(monthDate.lengthOfMonth());
            
            LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
            LocalDateTime monthEndDateTime = monthEnd.atTime(23, 59, 59);
            
            BigDecimal monthRevenue = businessCalculationEngine.getSalesValue(shopId, monthStartDateTime, monthEndDateTime);
            BigDecimal monthExpenses = businessCalculationEngine.getOperatingExpenses(shopId, monthStart, monthEnd);
            BigDecimal monthProfit = businessCalculationEngine.getGrossProfit(shopId, monthStartDateTime, monthEndDateTime);
            BigDecimal monthNetProfit = businessCalculationEngine.getNetProfit(shopId, monthStartDateTime, monthEndDateTime);
            
            yearlyOverview.add(FinancialDashboardResponse.YearlyOverviewData.builder()
                    .month(months[monthDate.getMonthValue() - 1] + " " + monthDate.getYear())
                    .revenue(monthRevenue)
                    .expenses(monthExpenses)
                    .profit(monthProfit)
                    .netProfit(monthNetProfit)
                    .build());
        }

        // Generate 5-year overview
        List<FinancialDashboardResponse.YearlyOverviewData> fiveYearOverview = new ArrayList<>();
        int currentYear = today.getYear();
        
        for (int i = 4; i >= 0; i--) {
            int year = currentYear - i;
            LocalDate yearStart = LocalDate.of(year, 1, 1);
            LocalDate yearEnd = LocalDate.of(year, 12, 31);
            
            LocalDateTime yearStartDateTime = yearStart.atStartOfDay();
            LocalDateTime yearEndDateTime = yearEnd.atTime(23, 59, 59);
            
            BigDecimal yearRevenue = businessCalculationEngine.getSalesValue(shopId, yearStartDateTime, yearEndDateTime);
            BigDecimal yearExpenses = businessCalculationEngine.getOperatingExpenses(shopId, yearStart, yearEnd);
            BigDecimal yearProfit = businessCalculationEngine.getGrossProfit(shopId, yearStartDateTime, yearEndDateTime);
            BigDecimal yearNetProfit = businessCalculationEngine.getNetProfit(shopId, yearStartDateTime, yearEndDateTime);
            
            fiveYearOverview.add(FinancialDashboardResponse.YearlyOverviewData.builder()
                    .month(String.valueOf(year))
                    .revenue(yearRevenue)
                    .expenses(yearExpenses)
                    .profit(yearProfit)
                    .netProfit(yearNetProfit)
                    .build());
        }

        return FinancialDashboardResponse.builder()
                .generatedAt(LocalDateTime.now())
                .currency("INR")
                .dateRange(FinancialDashboardResponse.DateRangeDTO.builder()
                        .from(dateRange.getFrom())
                        .to(dateRange.getTo())
                        .build())
                .kpi(kpi)
                .revenueVsExpenseTrend(revenueVsExpenseTrend)
                .profitTrend(profitTrendList)
                .expenseCategories(expenseCategories)
                .yearlyOverview(yearlyOverview)
                .fiveYearOverview(fiveYearOverview)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeResponse getIncome(Long shopId, String filter) {
        DateFilterUtil.DateRange dateRange = DateFilterUtil.getDateRange(filter);
        boolean isYearly = filter.equalsIgnoreCase("thisyear");

        // Get income data using BusinessCalculationEngine
        BigDecimal totalIncome = businessCalculationEngine.getSalesValue(shopId, dateRange.getFrom(), dateRange.getTo());
        
        // Calculate today's income using engine
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = LocalDateTime.now();
        BigDecimal todayIncome = businessCalculationEngine.getSalesValue(shopId, todayStart, todayEnd);
        
        // Calculate monthly income using engine
        LocalDateTime monthStart = dateRange.getFrom().toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = dateRange.getTo();
        BigDecimal monthlyIncome = businessCalculationEngine.getSalesValue(shopId, monthStart, monthEnd);
        
        // Calculate yearly income using engine
        LocalDateTime yearStart = dateRange.getFrom().toLocalDate().withDayOfYear(1).atStartOfDay();
        LocalDateTime yearEnd = dateRange.getTo();
        BigDecimal yearlyIncome = businessCalculationEngine.getSalesValue(shopId, yearStart, yearEnd);
        
        // Calculate average sale using engine
        Long vehiclesSold = businessCalculationEngine.getVehiclesSold(shopId, dateRange.getFrom(), dateRange.getTo());
        BigDecimal averageSale = businessCalculationEngine.getAverageSellingPrice(shopId, dateRange.getFrom(), dateRange.getTo());

        IncomeResponse.IncomeKPI kpi = IncomeResponse.IncomeKPI.builder()
                .todayIncome(todayIncome)
                .monthlyIncome(monthlyIncome)
                .yearlyIncome(yearlyIncome)
                .averageSale(averageSale)
                .build();

        // Income sources - currently only bike sales
        List<IncomeResponse.IncomeSourceData> incomeSources = new ArrayList<>();
        incomeSources.add(IncomeResponse.IncomeSourceData.builder()
                .source("Bike Sales")
                .amount(totalIncome)
                .percentage(BigDecimal.valueOf(100))
                .build());

        // Income trend from engine
        List<BusinessCalculationEngine.RevenueTrendData> incomeTrendData = 
            businessCalculationEngine.getRevenueTrend(shopId, dateRange.getFrom(), dateRange.getTo(), isYearly);
        List<IncomeResponse.IncomeTrendData> incomeTrend = incomeTrendData.stream()
            .map(itd -> IncomeResponse.IncomeTrendData.builder()
                    .period(itd.getPeriod())
                    .amount(itd.getRevenue())
                    .build())
            .collect(Collectors.toList());

        return IncomeResponse.builder()
                .generatedAt(LocalDateTime.now())
                .currency("INR")
                .dateRange(IncomeResponse.DateRangeDTO.builder()
                        .from(dateRange.getFrom())
                        .to(dateRange.getTo())
                        .build())
                .kpi(kpi)
                .incomeSources(incomeSources)
                .incomeTrend(incomeTrend)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceExpensesResponse getFinanceExpenses(Long shopId, String filter) {
        DateFilterUtil.DateRange dateRange = DateFilterUtil.getDateRange(filter);
        boolean isYearly = filter.equalsIgnoreCase("thisyear");

        // Get expense data using BusinessCalculationEngine
        BigDecimal monthlyExpenses = businessCalculationEngine.getOperatingExpenses(shopId, dateRange.getFrom().toLocalDate(), dateRange.getTo().toLocalDate());
        
        // Get highest expense from engine (simplified - would need enhanced engine method)
        List<Object[]> expenseCategoriesData = expenseRepository.getExpenseCategories(shopId, dateRange.getFrom().toLocalDate(), dateRange.getTo().toLocalDate());
        BigDecimal highestExpense = BigDecimal.ZERO;
        String highestExpenseCategory = "None";
        for (Object[] row : expenseCategoriesData) {
            BigDecimal amount = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(((Number) row[1]).longValue());
            if (amount.compareTo(highestExpense) > 0) {
                highestExpense = amount;
                highestExpenseCategory = (String) row[0];
            }
        }

        FinanceExpensesResponse.FinanceExpensesKPI kpi = FinanceExpensesResponse.FinanceExpensesKPI.builder()
                .monthlyExpenses(monthlyExpenses)
                .highestExpense(highestExpense)
                .highestExpenseCategory(highestExpenseCategory)
                .build();

        // Category breakdown
        List<FinanceExpensesResponse.ExpenseCategoryData> categoryBreakdown = new ArrayList<>();
        BigDecimal totalExpenses = monthlyExpenses;
        for (Object[] row : expenseCategoriesData) {
            BigDecimal amount = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(((Number) row[1]).longValue());
            BigDecimal percentage = BigDecimal.ZERO;
            if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.divide(totalExpenses, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            }
            categoryBreakdown.add(FinanceExpensesResponse.ExpenseCategoryData.builder()
                    .category((String) row[0])
                    .amount(amount)
                    .percentage(percentage)
                    .build());
        }

        // Expense trend from engine
        List<BusinessCalculationEngine.ExpenseTrendData> expenseTrendData = 
            businessCalculationEngine.getExpenseTrend(shopId, dateRange.getFrom().toLocalDate(), dateRange.getTo().toLocalDate(), isYearly);
        List<FinanceExpensesResponse.ExpenseTrendData> expenseTrend = expenseTrendData.stream()
            .map(etd -> FinanceExpensesResponse.ExpenseTrendData.builder()
                    .period(etd.getPeriod())
                    .amount(etd.getExpenses())
                    .build())
            .collect(Collectors.toList());

        return FinanceExpensesResponse.builder()
                .generatedAt(LocalDateTime.now())
                .currency("INR")
                .dateRange(FinanceExpensesResponse.DateRangeDTO.builder()
                        .from(dateRange.getFrom())
                        .to(dateRange.getTo())
                        .build())
                .kpi(kpi)
                .categoryBreakdown(categoryBreakdown)
                .expenseTrend(expenseTrend)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfitResponse getProfit(Long shopId, String filter) {
        DateFilterUtil.DateRange dateRange = DateFilterUtil.getDateRange(filter);
        boolean isYearly = filter.equalsIgnoreCase("thisyear");

        // Get profit data using BusinessCalculationEngine (single source of truth)
        BigDecimal revenue = businessCalculationEngine.getSalesValue(shopId, dateRange.getFrom(), dateRange.getTo());
        BigDecimal cogs = businessCalculationEngine.getCOGS(shopId, dateRange.getFrom(), dateRange.getTo());
        BigDecimal grossProfit = businessCalculationEngine.getGrossProfit(shopId, dateRange.getFrom(), dateRange.getTo());
        BigDecimal operatingExpenses = businessCalculationEngine.getOperatingExpenses(shopId, dateRange.getFrom().toLocalDate(), dateRange.getTo().toLocalDate());
        BigDecimal netProfit = businessCalculationEngine.getNetProfit(shopId, dateRange.getFrom(), dateRange.getTo());
        BigDecimal profitMargin = businessCalculationEngine.getProfitMargin(shopId, dateRange.getFrom(), dateRange.getTo());

        ProfitResponse.ProfitBreakdown profitBreakdown = ProfitResponse.ProfitBreakdown.builder()
                .revenue(revenue)
                .cogs(cogs)
                .grossProfit(grossProfit)
                .operatingExpenses(operatingExpenses)
                .netProfit(netProfit)
                .profitMargin(profitMargin)
                .build();

        // Profit trend from engine
        List<BusinessCalculationEngine.ProfitTrendData> profitTrendData = 
            businessCalculationEngine.getProfitTrend(shopId, dateRange.getFrom(), dateRange.getTo(), isYearly);
        List<ProfitResponse.ProfitTrendData> profitTrend = profitTrendData.stream()
            .map(ptd -> ProfitResponse.ProfitTrendData.builder()
                    .period(ptd.getPeriod())
                    .grossProfit(ptd.getGrossProfit())
                    .netProfit(ptd.getNetProfit())
                    .build())
            .collect(Collectors.toList());

        return ProfitResponse.builder()
                .generatedAt(LocalDateTime.now())
                .currency("INR")
                .dateRange(ProfitResponse.DateRangeDTO.builder()
                        .from(dateRange.getFrom())
                        .to(dateRange.getTo())
                        .build())
                .profitBreakdown(profitBreakdown)
                .profitTrend(profitTrend)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CashFlowResponse getCashFlow(Long shopId, String filter) {
        DateFilterUtil.DateRange dateRange = DateFilterUtil.getDateRange(filter);
        boolean isYearly = filter.equalsIgnoreCase("thisyear");

        // Get cash flow data using BusinessCalculationEngine (single source of truth)
        BusinessCalculationEngine.CashFlowData cashFlowData = 
            businessCalculationEngine.getCashFlow(shopId, dateRange.getFrom(), dateRange.getTo());

        CashFlowResponse.CashFlowKPI kpi = CashFlowResponse.CashFlowKPI.builder()
                .totalMoneyIn(cashFlowData.getMoneyIn())
                .totalMoneyOut(cashFlowData.getMoneyOut())
                .netCashFlow(cashFlowData.getNetCashFlow())
                .build();

        // Money In breakdown - Bike Sales
        List<CashFlowResponse.MoneyInData> moneyIn = new ArrayList<>();
        moneyIn.add(CashFlowResponse.MoneyInData.builder()
                .source("Bike Sales")
                .amount(cashFlowData.getMoneyIn())
                .build());

        // Money Out breakdown - Vehicle Purchases + Expenses
        BigDecimal inventoryPurchased = businessCalculationEngine.getInventoryPurchased(shopId, dateRange.getFrom().toLocalDate(), dateRange.getTo().toLocalDate());
        BigDecimal operatingExpenses = businessCalculationEngine.getOperatingExpenses(shopId, dateRange.getFrom().toLocalDate(), dateRange.getTo().toLocalDate());
        
        List<CashFlowResponse.MoneyOutData> moneyOut = new ArrayList<>();
        moneyOut.add(CashFlowResponse.MoneyOutData.builder()
                .category("Vehicle Purchases")
                .amount(inventoryPurchased)
                .build());
        moneyOut.add(CashFlowResponse.MoneyOutData.builder()
                .category("Operating Expenses")
                .amount(operatingExpenses)
                .build());

        // Cash flow trend from engine
        List<BusinessCalculationEngine.CashFlowTrendData> cashFlowTrendData = 
            businessCalculationEngine.getCashFlowTrend(shopId, dateRange.getFrom(), dateRange.getTo(), isYearly);
        List<CashFlowResponse.CashFlowTrendData> cashFlowTrend = cashFlowTrendData.stream()
            .map(cftd -> CashFlowResponse.CashFlowTrendData.builder()
                    .period(cftd.getPeriod())
                    .moneyIn(cftd.getMoneyIn())
                    .moneyOut(cftd.getMoneyOut())
                    .netCashFlow(cftd.getNetCashFlow())
                    .build())
            .collect(Collectors.toList());

        return CashFlowResponse.builder()
                .generatedAt(LocalDateTime.now())
                .currency("INR")
                .dateRange(CashFlowResponse.DateRangeDTO.builder()
                        .from(dateRange.getFrom())
                        .to(dateRange.getTo())
                        .build())
                .kpi(kpi)
                .moneyIn(moneyIn)
                .moneyOut(moneyOut)
                .cashFlowTrend(cashFlowTrend)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OutstandingResponse getOutstanding(Long shopId) {
        // Get outstanding data using BusinessCalculationEngine (single source of truth)
        BusinessCalculationEngine.OutstandingData outstandingData = 
            businessCalculationEngine.getOutstanding(shopId);

        OutstandingResponse.OutstandingKPI kpi = OutstandingResponse.OutstandingKPI.builder()
                .totalReceivable(outstandingData.getTotalReceivable())
                .totalPayable(outstandingData.getTotalPayable())
                .netOutstanding(outstandingData.getNetOutstanding())
                .build();

        // Receivables from engine
        List<OutstandingResponse.ReceivableData> receivables = outstandingData.getReceivables().stream()
            .map(r -> OutstandingResponse.ReceivableData.builder()
                    .type(r.getType())
                    .name(r.getName())
                    .amount(r.getAmount())
                    .dueDate(null)
                    .build())
            .collect(Collectors.toList());

        // Payables from engine
        List<OutstandingResponse.PayableData> payables = outstandingData.getPayables().stream()
            .map(p -> OutstandingResponse.PayableData.builder()
                    .type(p.getType())
                    .name(p.getName())
                    .amount(p.getAmount())
                    .dueDate(null)
                    .build())
            .collect(Collectors.toList());

        return OutstandingResponse.builder()
                .generatedAt(LocalDateTime.now())
                .currency("INR")
                .kpi(kpi)
                .receivables(receivables)
                .payables(payables)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleInvestmentResponse getVehicleInvestment(Long shopId) {
        // Get vehicle investment data using BusinessCalculationEngine (single source of truth)
        BusinessCalculationEngine.VehicleInvestmentData investmentData = 
            businessCalculationEngine.getVehicleInvestment(shopId);

        VehicleInvestmentResponse.VehicleInvestmentKPI kpi = VehicleInvestmentResponse.VehicleInvestmentKPI.builder()
                .totalInvestment(investmentData.getTotalInvestment())
                .currentInventoryCost(investmentData.getCurrentInventoryCost())
                .currentInventorySellingValue(investmentData.getCurrentInventorySellingValue())
                .vehiclesSoldValue(investmentData.getVehiclesSoldValue())
                .expectedProfit(investmentData.getExpectedProfit())
                .expectedMargin(investmentData.getExpectedMargin())
                .roi(investmentData.getRoi())
                .unsoldInvestment(investmentData.getUnsoldInvestment())
                .averageCostPrice(investmentData.getAverageCostPrice())
                .averageSellingPrice(investmentData.getAverageSellingPrice())
                .build();

        return VehicleInvestmentResponse.builder()
                .generatedAt(LocalDateTime.now())
                .currency("INR")
                .kpi(kpi)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummary(Long shopId) {
        // Get payment summary data using BusinessCalculationEngine (single source of truth)
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        BusinessCalculationEngine.PaymentSummaryData paymentData = 
            businessCalculationEngine.getPaymentSummary(shopId, monthStart, today);

        PaymentSummaryResponse.PaymentSummaryKPI kpi = PaymentSummaryResponse.PaymentSummaryKPI.builder()
                .receivedToday(paymentData.getReceivedToday())
                .receivedThisMonth(paymentData.getReceivedThisMonth())
                .pendingCustomerPayments(paymentData.getPendingCustomerPayments())
                .supplierPayables(paymentData.getSupplierPayables())
                .financeCompanyReceivables(paymentData.getFinanceCompanyReceivables())
                .build();

        return PaymentSummaryResponse.builder()
                .generatedAt(LocalDateTime.now())
                .currency("INR")
                .kpi(kpi)
                .build();
    }
}
