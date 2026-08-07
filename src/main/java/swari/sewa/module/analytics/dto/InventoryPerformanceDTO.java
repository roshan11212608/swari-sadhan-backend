package swari.sewa.module.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class InventoryPerformanceDTO {
    private InventoryPerformanceKPI kpi;
    private Map<String, Long> stockAgeAnalysis;
    private List<StockAgeTrendData> stockAgeTrend;

    @Data
    @Builder
    public static class InventoryPerformanceKPI {
        private BigDecimal inventoryTurnover;
        private BigDecimal daysInInventory;
        private BigDecimal deadStockValue;
        private Long deadStockCount;
        private BigDecimal averageCostPrice;
        private BigDecimal averageSellingPrice;
        private BigDecimal averageProfitPerVehicle;
        private BigDecimal profitMargin;
    }

    @Data
    @Builder
    public static class StockAgeTrendData {
        private String period;
        private Long age0to30;
        private Long age31to60;
        private Long age61to90;
        private Long age90Plus;
    }
}
