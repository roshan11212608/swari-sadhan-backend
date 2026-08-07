package swari.sewa.module.finance.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class VehicleInvestmentResponse {
    private LocalDateTime generatedAt;
    private String currency;
    private VehicleInvestmentKPI kpi;

    @Data
    @Builder
    public static class VehicleInvestmentKPI {
        private BigDecimal totalInvestment;
        private BigDecimal currentInventoryCost;
        private BigDecimal currentInventorySellingValue;
        private BigDecimal vehiclesSoldValue;
        private BigDecimal expectedProfit;
        private BigDecimal expectedMargin;
        private BigDecimal roi;
        private BigDecimal unsoldInvestment;
        private BigDecimal averageCostPrice;
        private BigDecimal averageSellingPrice;
    }
}
