package swari.sewa.module.analytics.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.analytics.dto.*;
import swari.sewa.module.analytics.engine.BusinessCalculationEngine;
import swari.sewa.module.analytics.service.AnalyticsService;
import swari.sewa.module.analytics.util.ChartDataUtil;
import swari.sewa.module.analytics.util.DateFilterUtil;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AnalyticsServiceImpl implements AnalyticsService {

    private final VehicleRepository vehicleRepository;
    private final BusinessCalculationEngine businessCalculationEngine;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsDashboardResponse getDashboard(Long shopId, String filter) {
        DateFilterUtil.DateRange dateRange = DateFilterUtil.getDateRange(filter);
        boolean isYearly = filter.equalsIgnoreCase("thisyear");

        // ── Compute shared base values ONCE to avoid redundant queries ──
        // Before: buildBusinessOverview, buildSalesInventory, buildInventoryPerformance
        //         each independently queried the same engine methods.
        //         getSalesValue was called 4×, getVehiclesSold 3×, getCurrentStock 2×,
        //         getGrossProfit 3×, getCOGS 3×, getOperatingExpenses 2×, getInventoryValue 2×
        // After: each base value is queried once and passed through to all builders.

        LocalDate periodStart = dateRange.getFrom().toLocalDate();
        LocalDate periodEnd = dateRange.getTo().toLocalDate();
        LocalDateTime periodStartDt = dateRange.getFrom();
        LocalDateTime periodEndDt = dateRange.getTo();

        // Period-filtered values
        Long vehiclesPurchased = businessCalculationEngine.getVehiclesPurchased(shopId, periodStart, periodEnd);
        Long vehiclesSold = businessCalculationEngine.getVehiclesSold(shopId, periodStartDt, periodEndDt);
        BigDecimal salesValue = businessCalculationEngine.getSalesValue(shopId, periodStartDt, periodEndDt);
        BigDecimal inventoryPurchased = businessCalculationEngine.getInventoryPurchased(shopId, periodStart, periodEnd);
        BigDecimal cogs = businessCalculationEngine.getCOGS(shopId, periodStartDt, periodEndDt);
        BigDecimal operatingExpenses = businessCalculationEngine.getOperatingExpenses(shopId, periodStart, periodEnd);
        BigDecimal grossProfit = salesValue.subtract(cogs);

        // Point-in-time values (not date-filtered)
        Long currentStock = businessCalculationEngine.getCurrentStock(shopId);
        Long availableStock = businessCalculationEngine.getAvailableStock(shopId);
        Long reservedStock = businessCalculationEngine.getReservedStock(shopId);
        BigDecimal inventoryValue = businessCalculationEngine.getInventoryValue(shopId);
        BigDecimal deadStockValue = businessCalculationEngine.getDeadStockValue(shopId);
        Map<String, Long> stockAgeAnalysis = businessCalculationEngine.getStockAgeAnalysis(shopId);
        Long deadStockCount = stockAgeAnalysis.getOrDefault("90+", 0L);

        // Derived values (computed in-memory, no additional queries)
        BigDecimal netProfit = grossProfit.subtract(operatingExpenses);
        BigDecimal profitMargin = BigDecimal.ZERO;
        if (salesValue.compareTo(BigDecimal.ZERO) != 0) {
            profitMargin = grossProfit.divide(salesValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }
        BigDecimal inventoryTurnover = BigDecimal.ZERO;
        if (inventoryValue.compareTo(BigDecimal.ZERO) != 0) {
            inventoryTurnover = cogs.divide(inventoryValue, 4, RoundingMode.HALF_UP);
        }
        BigDecimal daysInInventory = BigDecimal.ZERO;
        if (inventoryTurnover.compareTo(BigDecimal.ZERO) != 0) {
            daysInInventory = BigDecimal.valueOf(365).divide(inventoryTurnover, 2, RoundingMode.HALF_UP);
        }
        BigDecimal averageCostPrice = BigDecimal.ZERO;
        if (vehiclesPurchased != null && vehiclesPurchased > 0) {
            averageCostPrice = inventoryPurchased.divide(BigDecimal.valueOf(vehiclesPurchased), 2, RoundingMode.HALF_UP);
        }
        BigDecimal averageSellingPrice = BigDecimal.ZERO;
        if (vehiclesSold != null && vehiclesSold > 0) {
            averageSellingPrice = salesValue.divide(BigDecimal.valueOf(vehiclesSold), 2, RoundingMode.HALF_UP);
        }
        BigDecimal averageProfitPerVehicle = BigDecimal.ZERO;
        if (vehiclesSold != null && vehiclesSold > 0) {
            averageProfitPerVehicle = grossProfit.divide(BigDecimal.valueOf(vehiclesSold), 2, RoundingMode.HALF_UP);
        }

        // Build the three sections using the pre-computed shared values
        BusinessOverviewDTO businessOverview = buildBusinessOverview(
                shopId, dateRange, isYearly, vehiclesPurchased, vehiclesSold, currentStock,
                salesValue, inventoryPurchased, grossProfit);

        SalesInventoryDTO salesInventory = buildSalesInventory(
                shopId, dateRange, isYearly, salesValue, currentStock, availableStock,
                reservedStock, vehiclesSold, inventoryValue);

        InventoryPerformanceDTO inventoryPerformance = buildInventoryPerformance(
                inventoryTurnover, daysInInventory, deadStockValue, deadStockCount,
                stockAgeAnalysis, averageCostPrice, averageSellingPrice,
                averageProfitPerVehicle, profitMargin);

        return AnalyticsDashboardResponse.builder()
                .generatedAt(LocalDateTime.now())
                .currency("INR")
                .dateRange(AnalyticsDashboardResponse.DateRangeDTO.builder()
                        .from(dateRange.getFrom())
                        .to(dateRange.getTo())
                        .build())
                .businessOverview(businessOverview)
                .salesInventory(salesInventory)
                .inventoryPerformance(inventoryPerformance)
                .build();
    }

    private BusinessOverviewDTO buildBusinessOverview(Long shopId, DateFilterUtil.DateRange dateRange, boolean isYearly,
            Long vehiclesPurchased, Long vehiclesSold, Long currentStock,
            BigDecimal salesValue, BigDecimal inventoryPurchased, BigDecimal grossProfit) {

        BusinessOverviewDTO.BusinessOverviewKPI kpi = BusinessOverviewDTO.BusinessOverviewKPI.builder()
                .vehiclesPurchased(vehiclesPurchased.intValue())
                .vehiclesSold(vehiclesSold.intValue())
                .currentStock(currentStock.intValue())
                .salesValue(salesValue)
                .inventoryPurchased(inventoryPurchased)
                .grossProfit(grossProfit)
                .build();

        // Get trend data
        List<Object[]> trendData = vehicleRepository.getSalesPurchaseTrend(shopId, dateRange.getFrom(), dateRange.getTo(), isYearly);
        // Fill missing periods with zero for complete timeline
        List<Object[]> filledTrendData = ChartDataUtil.fillMissingPeriodsTwoValues(
            trendData, dateRange.getFrom(), dateRange.getTo(), isYearly);
        List<BusinessOverviewDTO.SalesPurchaseTrendData> trend = new ArrayList<>();
        for (Object[] row : filledTrendData) {
            trend.add(BusinessOverviewDTO.SalesPurchaseTrendData.builder()
                    .period((String) row[0])
                    .sales(row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(((Number) row[1]).longValue()))
                    .purchases(row[2] instanceof BigDecimal ? (BigDecimal) row[2] : new BigDecimal(((Number) row[2]).longValue()))
                    .build());
        }

        return BusinessOverviewDTO.builder()
                .kpi(kpi)
                .trend(trend)
                .build();
    }

    private SalesInventoryDTO buildSalesInventory(Long shopId, DateFilterUtil.DateRange dateRange, boolean isYearly,
            BigDecimal totalSales, Long currentStock, Long availableStock, Long reservedStock,
            Long soldStock, BigDecimal inventoryValue) {

        SalesInventoryDTO.SalesInventoryKPI kpi = SalesInventoryDTO.SalesInventoryKPI.builder()
                .totalSales(totalSales)
                .currentStock(currentStock.intValue())
                .availableStock(availableStock.intValue())
                .reservedStock(reservedStock.intValue())
                .soldStock(soldStock.intValue())
                .inventoryValue(inventoryValue)
                .build();

        // Get trend data
        List<Object[]> trendData = vehicleRepository.getSalesTrend(shopId, dateRange.getFrom(), dateRange.getTo(), isYearly);
        // Fill missing periods with zero for complete timeline
        List<Object[]> filledTrendData = ChartDataUtil.fillMissingPeriods(
            trendData, dateRange.getFrom(), dateRange.getTo(), isYearly);
        List<SalesInventoryDTO.SalesTrendData> trend = new ArrayList<>();
        for (Object[] row : filledTrendData) {
            trend.add(SalesInventoryDTO.SalesTrendData.builder()
                    .period((String) row[0])
                    .sales(row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(((Number) row[1]).longValue()))
                    .build());
        }

        return SalesInventoryDTO.builder()
                .kpi(kpi)
                .trend(trend)
                .build();
    }

    private InventoryPerformanceDTO buildInventoryPerformance(
            BigDecimal inventoryTurnover, BigDecimal daysInInventory, BigDecimal deadStockValue,
            Long deadStockCount, Map<String, Long> stockAgeAnalysis,
            BigDecimal averageCostPrice, BigDecimal averageSellingPrice,
            BigDecimal averageProfitPerVehicle, BigDecimal profitMargin) {

        InventoryPerformanceDTO.InventoryPerformanceKPI kpi = InventoryPerformanceDTO.InventoryPerformanceKPI.builder()
                .inventoryTurnover(inventoryTurnover)
                .daysInInventory(daysInInventory)
                .deadStockValue(deadStockValue)
                .deadStockCount(deadStockCount)
                .averageCostPrice(averageCostPrice)
                .averageSellingPrice(averageSellingPrice)
                .averageProfitPerVehicle(averageProfitPerVehicle)
                .profitMargin(profitMargin)
                .build();

        return InventoryPerformanceDTO.builder()
                .kpi(kpi)
                .stockAgeAnalysis(stockAgeAnalysis)
                .stockAgeTrend(new ArrayList<>()) // Can be enhanced with historical stock age data
                .build();
    }
}
