package swari.sewa.module.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessOverviewDTO {
    private BusinessOverviewKPI kpi;
    private List<SalesPurchaseTrendData> trend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessOverviewKPI {
        private Integer vehiclesPurchased;
        private Integer vehiclesSold;
        private Integer currentStock;
        private BigDecimal salesValue;
        private BigDecimal inventoryPurchased;
        private BigDecimal grossProfit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesPurchaseTrendData {
        private String period;
        private BigDecimal sales;
        private BigDecimal purchases;
    }
}
