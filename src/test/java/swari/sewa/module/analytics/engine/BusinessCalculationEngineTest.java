package swari.sewa.module.analytics.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.module.analytics.engine.impl.BusinessCalculationEngineImpl;
import swari.sewa.module.expense.repository.ExpenseRepository;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessCalculationEngineTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private BusinessCalculationEngineImpl businessCalculationEngine;

    private Long shopId = 1L;
    private LocalDateTime startDate = LocalDateTime.of(2026, 1, 1, 0, 0);
    private LocalDateTime endDate = LocalDateTime.of(2026, 1, 31, 23, 59);

    @Test
    void testGetCurrentStock() {
        when(vehicleRepository.countByShopIdAndStatusIn(eq(shopId), anyList())).thenReturn(20L);
        assertEquals(20L, businessCalculationEngine.getCurrentStock(shopId));
    }

    @Test
    void testGetInventoryValue_Null() {
        when(vehicleRepository.sumPurchasePriceByShopIdAndStatusIn(eq(shopId), anyList())).thenReturn(null);
        assertEquals(BigDecimal.ZERO, businessCalculationEngine.getInventoryValue(shopId));
    }

    @Test
    void testGetGrossProfit() {
        when(vehicleRepository.sumPriceByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("500000"));
        when(vehicleRepository.sumCOGSByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("350000"));
        assertEquals(new BigDecimal("150000"), businessCalculationEngine.getGrossProfit(shopId, startDate, endDate));
    }

    @Test
    void testGetGrossProfit_Loss() {
        when(vehicleRepository.sumPriceByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("300000"));
        when(vehicleRepository.sumCOGSByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("350000"));
        assertEquals(new BigDecimal("-50000"), businessCalculationEngine.getGrossProfit(shopId, startDate, endDate));
    }

    @Test
    void testGetNetProfit() {
        when(vehicleRepository.sumPriceByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("500000"));
        when(vehicleRepository.sumCOGSByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("350000"));
        when(expenseRepository.sumAmountByShopIdAndExpenseDateBetween(eq(shopId), any(LocalDate.class), any(LocalDate.class))).thenReturn(new BigDecimal("100000"));
        assertEquals(new BigDecimal("50000"), businessCalculationEngine.getNetProfit(shopId, startDate, endDate));
    }

    @Test
    void testGetAveragePurchasePrice_NoVehicles() {
        when(vehicleRepository.countByShopIdAndBoughtDateBetween(eq(shopId), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        assertEquals(BigDecimal.ZERO, businessCalculationEngine.getAveragePurchasePrice(shopId, startDate.toLocalDate(), endDate.toLocalDate()));
    }

    @Test
    void testGetProfitMargin_NoSales() {
        when(vehicleRepository.sumPriceByShopIdAndStatusAndSoldAtBetween(eq(shopId), eq(VehicleStatus.SOLD), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, businessCalculationEngine.getProfitMargin(shopId, startDate, endDate));
    }

    // ==================== TREND EDGE CASES ====================

    @Test
    void testGetProfitTrend_EmptyShowroom() {
        when(vehicleRepository.getProfitTrend(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class), anyBoolean()))
            .thenReturn(Collections.emptyList());

        List<BusinessCalculationEngine.ProfitTrendData> result =
            businessCalculationEngine.getProfitTrend(shopId, startDate, endDate, false);

        assertEquals(1, result.size());
        assertEquals("Jan", result.get(0).getPeriod());
        assertEquals(BigDecimal.ZERO, result.get(0).getGrossProfit());
        assertEquals(BigDecimal.ZERO, result.get(0).getNetProfit());
    }

    @Test
    void testGetProfitTrend_SingleMonth() {
        when(vehicleRepository.getProfitTrend(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class), anyBoolean()))
            .thenReturn(Collections.singletonList(new Object[]{"Jan", new BigDecimal("1000"), new BigDecimal("800")}));

        List<BusinessCalculationEngine.ProfitTrendData> result =
            businessCalculationEngine.getProfitTrend(shopId, startDate, endDate, false);

        assertEquals(1, result.size());
        assertEquals("Jan", result.get(0).getPeriod());
        assertEquals(new BigDecimal("1000"), result.get(0).getGrossProfit());
        assertEquals(new BigDecimal("800"), result.get(0).getNetProfit());
    }

    @Test
    void testGetProfitTrend_TwoMonthsFillsMissing() {
        LocalDateTime janStart = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime febEnd = LocalDateTime.of(2026, 2, 28, 23, 59);
        when(vehicleRepository.getProfitTrend(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class), anyBoolean()))
            .thenReturn(Collections.singletonList(new Object[]{"Jan", new BigDecimal("1000"), new BigDecimal("800")}));

        List<BusinessCalculationEngine.ProfitTrendData> result =
            businessCalculationEngine.getProfitTrend(shopId, janStart, febEnd, false);

        assertEquals(2, result.size());
        assertEquals("Jan", result.get(0).getPeriod());
        assertEquals(new BigDecimal("1000"), result.get(0).getGrossProfit());
        assertEquals("Feb", result.get(1).getPeriod());
        assertEquals(BigDecimal.ZERO, result.get(1).getGrossProfit());
        assertEquals(BigDecimal.ZERO, result.get(1).getNetProfit());
    }

    @Test
    void testGetProfitTrend_MultipleMonthsFillsMiddle() {
        LocalDateTime janStart = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime marEnd = LocalDateTime.of(2026, 3, 31, 23, 59);
        when(vehicleRepository.getProfitTrend(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class), anyBoolean()))
            .thenReturn(Arrays.asList(
                new Object[]{"Jan", new BigDecimal("1000"), new BigDecimal("800")},
                new Object[]{"Mar", new BigDecimal("1500"), new BigDecimal("1100")}
            ));

        List<BusinessCalculationEngine.ProfitTrendData> result =
            businessCalculationEngine.getProfitTrend(shopId, janStart, marEnd, false);

        assertEquals(3, result.size());
        assertEquals("Jan", result.get(0).getPeriod());
        assertEquals(new BigDecimal("1000"), result.get(0).getGrossProfit());
        assertEquals("Feb", result.get(1).getPeriod());
        assertEquals(BigDecimal.ZERO, result.get(1).getGrossProfit());
        assertEquals("Mar", result.get(2).getPeriod());
        assertEquals(new BigDecimal("1500"), result.get(2).getGrossProfit());
    }

    @Test
    void testGetRevenueTrend_EmptyShowroom() {
        when(vehicleRepository.getSalesTrend(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class), anyBoolean()))
            .thenReturn(Collections.emptyList());

        List<BusinessCalculationEngine.RevenueTrendData> result =
            businessCalculationEngine.getRevenueTrend(shopId, startDate, endDate, false);

        assertEquals(1, result.size());
        assertEquals("Jan", result.get(0).getPeriod());
        assertEquals(BigDecimal.ZERO, result.get(0).getRevenue());
    }

    @Test
    void testGetRevenueTrend_MultipleMonthsFillsMissing() {
        LocalDateTime janStart = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime marEnd = LocalDateTime.of(2026, 3, 31, 23, 59);
        when(vehicleRepository.getSalesTrend(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class), anyBoolean()))
            .thenReturn(Arrays.asList(
                new Object[]{"Jan", new BigDecimal("5000")},
                new Object[]{"Mar", new BigDecimal("7000")}
            ));

        List<BusinessCalculationEngine.RevenueTrendData> result =
            businessCalculationEngine.getRevenueTrend(shopId, janStart, marEnd, false);

        assertEquals(3, result.size());
        assertEquals("Jan", result.get(0).getPeriod());
        assertEquals(new BigDecimal("5000"), result.get(0).getRevenue());
        assertEquals("Feb", result.get(1).getPeriod());
        assertEquals(BigDecimal.ZERO, result.get(1).getRevenue());
        assertEquals("Mar", result.get(2).getPeriod());
        assertEquals(new BigDecimal("7000"), result.get(2).getRevenue());
    }

    @Test
    void testGetExpenseTrend_EmptyShowroom() {
        when(expenseRepository.getExpenseTrend(eq(shopId), any(LocalDate.class), any(LocalDate.class), anyBoolean()))
            .thenReturn(Collections.emptyList());

        List<BusinessCalculationEngine.ExpenseTrendData> result =
            businessCalculationEngine.getExpenseTrend(shopId, startDate.toLocalDate(), endDate.toLocalDate(), false);

        assertEquals(1, result.size());
        assertEquals("Jan", result.get(0).getPeriod());
        assertEquals(BigDecimal.ZERO, result.get(0).getExpenses());
    }

    @Test
    void testGetExpenseTrend_MultipleMonthsFillsMissing() {
        LocalDate janStart = LocalDate.of(2026, 1, 1);
        LocalDate marEnd = LocalDate.of(2026, 3, 31);
        when(expenseRepository.getExpenseTrend(eq(shopId), any(LocalDate.class), any(LocalDate.class), anyBoolean()))
            .thenReturn(Arrays.asList(
                new Object[]{"Jan", new BigDecimal("2000")},
                new Object[]{"Mar", new BigDecimal("3000")}
            ));

        List<BusinessCalculationEngine.ExpenseTrendData> result =
            businessCalculationEngine.getExpenseTrend(shopId, janStart, marEnd, false);

        assertEquals(3, result.size());
        assertEquals("Jan", result.get(0).getPeriod());
        assertEquals(new BigDecimal("2000"), result.get(0).getExpenses());
        assertEquals("Feb", result.get(1).getPeriod());
        assertEquals(BigDecimal.ZERO, result.get(1).getExpenses());
        assertEquals("Mar", result.get(2).getPeriod());
        assertEquals(new BigDecimal("3000"), result.get(2).getExpenses());
    }

    @Test
    void testGetCashFlowTrend_EmptyShowroom() {
        when(vehicleRepository.getCashFlowTrend(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class), anyBoolean()))
            .thenReturn(Collections.emptyList());

        List<BusinessCalculationEngine.CashFlowTrendData> result =
            businessCalculationEngine.getCashFlowTrend(shopId, startDate, endDate, false);

        assertEquals(1, result.size());
        assertEquals("Jan", result.get(0).getPeriod());
        assertEquals(BigDecimal.ZERO, result.get(0).getMoneyIn());
        assertEquals(BigDecimal.ZERO, result.get(0).getMoneyOut());
        assertEquals(BigDecimal.ZERO, result.get(0).getNetCashFlow());
    }

    @Test
    void testGetCashFlowTrend_MultipleMonthsFillsMissing() {
        LocalDateTime janStart = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime marEnd = LocalDateTime.of(2026, 3, 31, 23, 59);
        when(vehicleRepository.getCashFlowTrend(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class), anyBoolean()))
            .thenReturn(Arrays.asList(
                new Object[]{"Jan", new BigDecimal("5000"), new BigDecimal("1000")},
                new Object[]{"Mar", new BigDecimal("6000"), new BigDecimal("2000")}
            ));

        List<BusinessCalculationEngine.CashFlowTrendData> result =
            businessCalculationEngine.getCashFlowTrend(shopId, janStart, marEnd, false);

        assertEquals(3, result.size());
        assertEquals("Jan", result.get(0).getPeriod());
        assertEquals(new BigDecimal("5000"), result.get(0).getMoneyIn());
        assertEquals(new BigDecimal("1000"), result.get(0).getMoneyOut());
        assertEquals(new BigDecimal("4000"), result.get(0).getNetCashFlow());
        assertEquals("Feb", result.get(1).getPeriod());
        assertEquals(BigDecimal.ZERO, result.get(1).getMoneyIn());
        assertEquals(BigDecimal.ZERO, result.get(1).getMoneyOut());
        assertEquals(BigDecimal.ZERO, result.get(1).getNetCashFlow());
        assertEquals("Mar", result.get(2).getPeriod());
        assertEquals(new BigDecimal("6000"), result.get(2).getMoneyIn());
        assertEquals(new BigDecimal("2000"), result.get(2).getMoneyOut());
        assertEquals(new BigDecimal("4000"), result.get(2).getNetCashFlow());
    }
}
