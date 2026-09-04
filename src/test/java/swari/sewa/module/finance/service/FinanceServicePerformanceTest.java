package swari.sewa.module.finance.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import swari.sewa.module.analytics.engine.BusinessCalculationEngine;
import swari.sewa.module.expense.repository.ExpenseRepository;
import swari.sewa.module.finance.dto.FinancialDashboardResponse;
import swari.sewa.module.finance.service.impl.FinanceServiceImpl;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Regression tests for FinanceServiceImpl performance optimizations.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>getFinancialDashboard uses GROUP BY overview queries instead of per-month engine calls</li>
 *   <li>KPI queries are deduplicated (getSalesValue called once, not 4×)</li>
 *   <li>getProfit deduplicates KPI queries (getSalesValue + getCOGS + getOperatingExpenses once each)</li>
 *   <li>Yearly overview returns 12 months of data</li>
 *   <li>5-year overview returns 5 years of data</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class FinanceServicePerformanceTest {

    @Mock private BusinessCalculationEngine businessCalculationEngine;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private VehicleRepository vehicleRepository;

    @InjectMocks private FinanceServiceImpl financeService;

    @Test
    void getFinancialDashboard_usesGroupByOverviewQueries_notPerMonthEngineCalls() {
        Long shopId = 1L;

        // KPI base values (3 queries instead of ~13)
        when(businessCalculationEngine.getSalesValue(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("500000"));
        when(businessCalculationEngine.getCOGS(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("350000"));
        when(businessCalculationEngine.getOperatingExpenses(eq(shopId), any(), any()))
                .thenReturn(new BigDecimal("50000"));
        when(businessCalculationEngine.getInventoryPurchased(eq(shopId), any(), any()))
                .thenReturn(new BigDecimal("200000"));

        // Trend queries (3 queries)
        when(businessCalculationEngine.getRevenueTrend(eq(shopId), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(businessCalculationEngine.getExpenseTrend(eq(shopId), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(businessCalculationEngine.getProfitTrend(eq(shopId), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        // Expense categories (1 query)
        when(expenseRepository.getExpenseCategories(eq(shopId), any(), any()))
                .thenReturn(Collections.emptyList());

        // Yearly overview GROUP BY queries (2 queries instead of ~96)
        when(vehicleRepository.getMonthlySalesProfitOverview(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(expenseRepository.getMonthlyExpenseOverview(eq(shopId), any(), any()))
                .thenReturn(Collections.emptyList());

        // 5-year overview GROUP BY queries (2 queries instead of ~40)
        when(vehicleRepository.getYearlySalesProfitOverview(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(expenseRepository.getYearlyExpenseOverview(eq(shopId), any(), any()))
                .thenReturn(Collections.emptyList());

        FinancialDashboardResponse response = financeService.getFinancialDashboard(shopId, "thismonth");

        assertNotNull(response);
        assertNotNull(response.getKpi());
        assertEquals(new BigDecimal("500000"), response.getKpi().getSalesRevenue());
        assertEquals(new BigDecimal("200000"), response.getKpi().getInventoryPurchase());
        assertEquals(new BigDecimal("50000"), response.getKpi().getOperatingExpenses());
        assertEquals(new BigDecimal("150000"), response.getKpi().getGrossProfit()); // 500000 - 350000
        assertEquals(new BigDecimal("100000"), response.getKpi().getNetProfit());   // 150000 - 50000

        // Verify GROUP BY overview queries were used
        verify(vehicleRepository).getMonthlySalesProfitOverview(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(expenseRepository).getMonthlyExpenseOverview(eq(shopId), any(), any());
        verify(vehicleRepository).getYearlySalesProfitOverview(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(expenseRepository).getYearlyExpenseOverview(eq(shopId), any(), any());

        // Verify KPI deduplication: getSalesValue should be called exactly once (for KPI base)
        // The trend calls use getRevenueTrend/getExpenseTrend/getProfitTrend which are separate methods
        verify(businessCalculationEngine, times(1)).getSalesValue(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, times(1)).getCOGS(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, times(1)).getOperatingExpenses(eq(shopId), any(), any());

        // Verify the old per-month methods are NOT called in a loop
        // getGrossProfit, getNetProfit, getProfitMargin should NOT be called (they're derived in-memory)
        verify(businessCalculationEngine, never()).getGrossProfit(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, never()).getNetProfit(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, never()).getProfitMargin(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getFinancialDashboard_yearlyOverview_returns12Months() {
        Long shopId = 1L;

        when(businessCalculationEngine.getSalesValue(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);
        when(businessCalculationEngine.getCOGS(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);
        when(businessCalculationEngine.getOperatingExpenses(eq(shopId), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(businessCalculationEngine.getInventoryPurchased(eq(shopId), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(businessCalculationEngine.getRevenueTrend(eq(shopId), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(businessCalculationEngine.getExpenseTrend(eq(shopId), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(businessCalculationEngine.getProfitTrend(eq(shopId), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(expenseRepository.getExpenseCategories(eq(shopId), any(), any()))
                .thenReturn(Collections.emptyList());
        when(vehicleRepository.getMonthlySalesProfitOverview(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(expenseRepository.getMonthlyExpenseOverview(eq(shopId), any(), any()))
                .thenReturn(Collections.emptyList());
        when(vehicleRepository.getYearlySalesProfitOverview(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(expenseRepository.getYearlyExpenseOverview(eq(shopId), any(), any()))
                .thenReturn(Collections.emptyList());

        FinancialDashboardResponse response = financeService.getFinancialDashboard(shopId, "thismonth");

        assertNotNull(response.getYearlyOverview());
        assertEquals(12, response.getYearlyOverview().size());
        assertNotNull(response.getFiveYearOverview());
        assertEquals(5, response.getFiveYearOverview().size());
    }

    @Test
    void getFinancialDashboard_yearlyOverview_mapsGroupByDataCorrectly() {
        Long shopId = 1L;

        when(businessCalculationEngine.getSalesValue(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);
        when(businessCalculationEngine.getCOGS(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);
        when(businessCalculationEngine.getOperatingExpenses(eq(shopId), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(businessCalculationEngine.getInventoryPurchased(eq(shopId), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(businessCalculationEngine.getRevenueTrend(eq(shopId), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(businessCalculationEngine.getExpenseTrend(eq(shopId), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(businessCalculationEngine.getProfitTrend(eq(shopId), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(expenseRepository.getExpenseCategories(eq(shopId), any(), any()))
                .thenReturn(Collections.emptyList());

        // Simulate one month of sales data
        String currentMonthYear = java.time.LocalDate.now().getMonth().toString().substring(0, 1).toUpperCase()
                + java.time.LocalDate.now().getMonth().toString().substring(1, 3).toLowerCase()
                + " " + java.time.LocalDate.now().getYear();
        // Use proper 3-letter month abbreviation
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        java.time.LocalDate today = java.time.LocalDate.now();
        String expectedPeriod = monthNames[today.getMonthValue() - 1] + " " + today.getYear();

        when(vehicleRepository.getMonthlySalesProfitOverview(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(new Object[]{expectedPeriod, new BigDecimal("100000"), new BigDecimal("30000")}));
        when(expenseRepository.getMonthlyExpenseOverview(eq(shopId), any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{expectedPeriod, new BigDecimal("10000")}));
        when(vehicleRepository.getYearlySalesProfitOverview(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(expenseRepository.getYearlyExpenseOverview(eq(shopId), any(), any()))
                .thenReturn(Collections.emptyList());

        FinancialDashboardResponse response = financeService.getFinancialDashboard(shopId, "thismonth");

        // The last month in yearlyOverview should have the data from our mock
        FinancialDashboardResponse.YearlyOverviewData lastMonth = response.getYearlyOverview().get(11);
        assertEquals(expectedPeriod, lastMonth.getMonth());
        assertEquals(new BigDecimal("100000"), lastMonth.getRevenue());
        assertEquals(new BigDecimal("10000"), lastMonth.getExpenses());
        assertEquals(new BigDecimal("30000"), lastMonth.getProfit());
        assertEquals(new BigDecimal("20000"), lastMonth.getNetProfit()); // 30000 - 10000
    }

    @Test
    void getProfit_deduplicatesKpiQueries() {
        Long shopId = 1L;

        when(businessCalculationEngine.getSalesValue(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("500000"));
        when(businessCalculationEngine.getCOGS(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("350000"));
        when(businessCalculationEngine.getOperatingExpenses(eq(shopId), any(), any()))
                .thenReturn(new BigDecimal("50000"));
        when(businessCalculationEngine.getProfitTrend(eq(shopId), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        var response = financeService.getProfit(shopId, "thismonth");

        assertNotNull(response);
        assertEquals(new BigDecimal("500000"), response.getProfitBreakdown().getRevenue());
        assertEquals(new BigDecimal("350000"), response.getProfitBreakdown().getCogs());
        assertEquals(new BigDecimal("150000"), response.getProfitBreakdown().getGrossProfit());
        assertEquals(new BigDecimal("100000"), response.getProfitBreakdown().getNetProfit());

        // Verify deduplication: each base query called exactly once
        verify(businessCalculationEngine, times(1)).getSalesValue(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, times(1)).getCOGS(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, times(1)).getOperatingExpenses(eq(shopId), any(), any());

        // Verify derived methods NOT called
        verify(businessCalculationEngine, never()).getGrossProfit(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, never()).getNetProfit(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, never()).getProfitMargin(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
    }
}
