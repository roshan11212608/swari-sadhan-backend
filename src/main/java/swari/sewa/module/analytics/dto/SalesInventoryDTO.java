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
public class SalesInventoryDTO {
    private SalesInventoryKPI kpi;
    private List<SalesTrendData> trend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesInventoryKPI {
        private BigDecimal totalSales;
        private Integer currentStock;
        private Integer availableStock;
        private Integer reservedStock;
        private Integer soldStock;
        private BigDecimal inventoryValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesTrendData {
        private String period;
        private BigDecimal sales;
    }
}
