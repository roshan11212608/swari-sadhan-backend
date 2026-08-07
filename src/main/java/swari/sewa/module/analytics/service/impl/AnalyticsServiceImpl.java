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

        // Get Business Overview
        BusinessOverviewDTO businessOverview = buildBusinessOverview(shopId, dateRange, isYearly);

        // Get Sales & Inventory
        SalesInventoryDTO salesInventory = buildSalesInventory(shopId, dateRange, isYearly);

        // Get Inventory Performance (focus on business performance metrics)
        InventoryPerformanceDTO inventoryPerformance = buildInventoryPerformance(shopId, dateRange, isYearly);

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

    private BusinessOverviewDTO buildBusinessOverview(Long shopId, DateFilterUtil.DateRange dateRange, boolean isYearly) {
        // Get KPI counts using BusinessCalculationEngine
        Long vehiclesPurchased = businessCalculationEngine.getVehiclesPurchased(
            shopId, dateRange.getFrom().toLocalDate(), dateRange.getTo().toLocalDate());
        Long vehiclesSold = businessCalculationEngine.getVehiclesSold(
            shopId, dateRange.getFrom(), dateRange.getTo());
        // Current Stock is point-in-time, does NOT respect date filter
        Long currentStock = businessCalculationEngine.getCurrentStock(shopId);
        
        // Get financial sums using BusinessCalculationEngine
        BigDecimal salesValue = businessCalculationEngine.getSalesValue(
            shopId, dateRange.getFrom(), dateRange.getTo());
        BigDecimal inventoryPurchased = businessCalculationEngine.getInventoryPurchased(
            shopId, dateRange.getFrom().toLocalDate(), dateRange.getTo().toLocalDate());
        BigDecimal grossProfit = businessCalculationEngine.getGrossProfit(
            shopId, dateRange.getFrom(), dateRange.getTo());

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

    private SalesInventoryDTO buildSalesInventory(Long shopId, DateFilterUtil.DateRange dateRange, boolean isYearly) {
        // Get KPI using BusinessCalculationEngine
        BigDecimal totalSales = businessCalculationEngine.getSalesValue(
            shopId, dateRange.getFrom(), dateRange.getTo());
        
        // Stock KPIs are point-in-time, do NOT respect date filter
        Long currentStock = businessCalculationEngine.getCurrentStock(shopId);
        Long availableStock = businessCalculationEngine.getAvailableStock(shopId);
        Long reservedStock = businessCalculationEngine.getReservedStock(shopId);
        Long soldStock = businessCalculationEngine.getVehiclesSold(
            shopId, dateRange.getFrom(), dateRange.getTo());
        
        // Inventory Value is point-in-time, does NOT respect date filter
        BigDecimal inventoryValue = businessCalculationEngine.getInventoryValue(shopId);

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

    private InventoryPerformanceDTO buildInventoryPerformance(Long shopId, DateFilterUtil.DateRange dateRange, boolean isYearly) {
        // Get inventory performance KPIs using BusinessCalculationEngine
        BigDecimal inventoryTurnover = businessCalculationEngine.getInventoryTurnover(
            shopId, dateRange.getFrom(), dateRange.getTo());
        BigDecimal daysInInventory = businessCalculationEngine.getDaysInInventory(
            shopId, dateRange.getFrom(), dateRange.getTo());
        BigDecimal deadStockValue = businessCalculationEngine.getDeadStockValue(shopId);
        
        // Calculate dead stock count (90+ days)
        Map<String, Long> stockAgeAnalysis = businessCalculationEngine.getStockAgeAnalysis(shopId);
        Long deadStockCount = stockAgeAnalysis.getOrDefault("90+", 0L);
        
        // Get average KPIs
        BigDecimal averageCostPrice = businessCalculationEngine.getAveragePurchasePrice(
            shopId, dateRange.getFrom().toLocalDate(), dateRange.getTo().toLocalDate());
        BigDecimal averageSellingPrice = businessCalculationEngine.getAverageSellingPrice(
            shopId, dateRange.getFrom(), dateRange.getTo());
        BigDecimal averageProfitPerVehicle = businessCalculationEngine.getAverageProfitPerVehicle(
            shopId, dateRange.getFrom(), dateRange.getTo());
        BigDecimal profitMargin = businessCalculationEngine.getProfitMargin(
            shopId, dateRange.getFrom(), dateRange.getTo());

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

        // Stock age analysis (point-in-time, no date filter)
        // This is already captured in kpi.deadStockValue and kpi.deadStockCount
        // We'll pass the full analysis for detailed breakdown
        return InventoryPerformanceDTO.builder()
                .kpi(kpi)
                .stockAgeAnalysis(stockAgeAnalysis)
                .stockAgeTrend(new ArrayList<>()) // Can be enhanced with historical stock age data
                .build();
    }
}
