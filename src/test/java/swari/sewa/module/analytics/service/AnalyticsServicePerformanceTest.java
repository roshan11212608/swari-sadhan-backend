package swari.sewa.module.analytics.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import swari.sewa.module.analytics.engine.BusinessCalculationEngine;
import swari.sewa.module.analytics.service.impl.AnalyticsServiceImpl;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Regression tests for AnalyticsServiceImpl performance optimizations.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>getDashboard deduplicates KPI queries (getSalesValue called once, not 4×)</li>
 *   <li>getVehiclesSold called once, not 3×</li>
 *   <li>getCurrentStock called once, not 2×</li>
 *   <li>getCOGS called once, not 3×</li>
 *   <li>getGrossProfit NOT called (derived in-memory from salesValue - cogs)</li>
 *   <li>getInventoryTurnover NOT called (derived in-memory from cogs / inventoryValue)</li>
 *   <li>getDaysInInventory NOT called (derived in-memory from inventoryTurnover)</li>
 *   <li>getProfitMargin NOT called (derived in-memory)</li>
 *   <li>getAverageSellingPrice NOT called (derived in-memory)</li>
 *   <li>getAverageProfitPerVehicle NOT called (derived in-memory)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServicePerformanceTest {

    @Mock private VehicleRepository vehicleRepository;
    @Mock private BusinessCalculationEngine businessCalculationEngine;

    @InjectMocks private AnalyticsServiceImpl analyticsService;

    @Test
    void getDashboard_deduplicatesKpiQueries() {
        Long shopId = 1L;

        // Period-filtered values
        when(businessCalculationEngine.getVehiclesPurchased(eq(shopId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(10L);
        when(businessCalculationEngine.getVehiclesSold(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);
        when(businessCalculationEngine.getSalesValue(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("500000"));
        when(businessCalculationEngine.getInventoryPurchased(eq(shopId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("200000"));
        when(businessCalculationEngine.getCOGS(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("350000"));
        when(businessCalculationEngine.getOperatingExpenses(eq(shopId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("50000"));

        // Point-in-time values
        when(businessCalculationEngine.getCurrentStock(eq(shopId))).thenReturn(20L);
        when(businessCalculationEngine.getAvailableStock(eq(shopId))).thenReturn(15L);
        when(businessCalculationEngine.getReservedStock(eq(shopId))).thenReturn(5L);
        when(businessCalculationEngine.getInventoryValue(eq(shopId))).thenReturn(new BigDecimal("400000"));
        when(businessCalculationEngine.getDeadStockValue(eq(shopId))).thenReturn(new BigDecimal("50000"));

        Map<String, Long> stockAge = new HashMap<>();
        stockAge.put("0-30", 5L);
        stockAge.put("31-60", 8L);
        stockAge.put("61-90", 4L);
        stockAge.put("90+", 3L);
        when(businessCalculationEngine.getStockAgeAnalysis(eq(shopId))).thenReturn(stockAge);

        // Trend queries
        when(vehicleRepository.getSalesPurchaseTrend(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(vehicleRepository.getSalesTrend(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class), anyBoolean()))
                .thenReturn(Collections.emptyList());

        var response = analyticsService.getDashboard(shopId, "thismonth");

        assertNotNull(response);
        assertNotNull(response.getBusinessOverview());
        assertNotNull(response.getSalesInventory());
        assertNotNull(response.getInventoryPerformance());

        // ── Verify deduplication: each base query called exactly once ──
        verify(businessCalculationEngine, times(1)).getSalesValue(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, times(1)).getVehiclesSold(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, times(1)).getCurrentStock(eq(shopId));
        verify(businessCalculationEngine, times(1)).getCOGS(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, times(1)).getOperatingExpenses(eq(shopId), any(LocalDate.class), any(LocalDate.class));
        verify(businessCalculationEngine, times(1)).getInventoryValue(eq(shopId));

        // ── Verify derived methods NOT called (computed in-memory) ──
        verify(businessCalculationEngine, never()).getGrossProfit(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, never()).getNetProfit(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, never()).getProfitMargin(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, never()).getInventoryTurnover(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, never()).getDaysInInventory(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, never()).getAverageSellingPrice(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, never()).getAverageProfitPerVehicle(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(businessCalculationEngine, never()).getAveragePurchasePrice(eq(shopId), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void getDashboard_computesDerivedValuesCorrectly() {
        Long shopId = 1L;

        when(businessCalculationEngine.getVehiclesPurchased(eq(shopId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(10L);
        when(businessCalculationEngine.getVehiclesSold(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);
        when(businessCalculationEngine.getSalesValue(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("500000"));
        when(businessCalculationEngine.getInventoryPurchased(eq(shopId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("200000"));
        when(businessCalculationEngine.getCOGS(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("350000"));
        when(businessCalculationEngine.getOperatingExpenses(eq(shopId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("50000"));
        when(businessCalculationEngine.getCurrentStock(eq(shopId))).thenReturn(20L);
        when(businessCalculationEngine.getAvailableStock(eq(shopId))).thenReturn(15L);
        when(businessCalculationEngine.getReservedStock(eq(shopId))).thenReturn(5L);
        when(businessCalculationEngine.getInventoryValue(eq(shopId))).thenReturn(new BigDecimal("400000"));
        when(businessCalculationEngine.getDeadStockValue(eq(shopId))).thenReturn(new BigDecimal("50000"));
        when(businessCalculationEngine.getStockAgeAnalysis(eq(shopId)))
                .thenReturn(Map.of("0-30", 5L, "31-60", 8L, "61-90", 4L, "90+", 3L));
        when(vehicleRepository.getSalesPurchaseTrend(eq(shopId), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(vehicleRepository.getSalesTrend(eq(shopId), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        var response = analyticsService.getDashboard(shopId, "thismonth");

        // Verify derived values are computed correctly
        var kpi = response.getInventoryPerformance().getKpi();
        // grossProfit = 500000 - 350000 = 150000
        // inventoryTurnover = 350000 / 400000 = 0.8750
        assertEquals(new BigDecimal("0.8750"), kpi.getInventoryTurnover());
        // daysInInventory = 365 / 0.8750 = 417.14
        assertEquals(new BigDecimal("417.14"), kpi.getDaysInInventory());
        // profitMargin = 150000 / 500000 * 100 = 30.0000
        assertEquals(new BigDecimal("30.0000"), kpi.getProfitMargin());
        // averageSellingPrice = 500000 / 5 = 100000.00
        assertEquals(new BigDecimal("100000.00"), kpi.getAverageSellingPrice());
        // averageProfitPerVehicle = 150000 / 5 = 30000.00
        assertEquals(new BigDecimal("30000.00"), kpi.getAverageProfitPerVehicle());
        // averageCostPrice = 200000 / 10 = 20000.00
        assertEquals(new BigDecimal("20000.00"), kpi.getAverageCostPrice());
        // deadStockCount from stockAgeAnalysis
        assertEquals(3L, kpi.getDeadStockCount());
    }
}
