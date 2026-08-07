package swari.sewa.module.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDashboardResponse {
    private LocalDateTime generatedAt;
    private String currency;
    private DateRangeDTO dateRange;
    private BusinessOverviewDTO businessOverview;
    private SalesInventoryDTO salesInventory;
    private InventoryPerformanceDTO inventoryPerformance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRangeDTO {
        private LocalDateTime from;
        private LocalDateTime to;
    }
}
