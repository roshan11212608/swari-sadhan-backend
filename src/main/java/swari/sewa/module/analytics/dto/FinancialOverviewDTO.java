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
public class FinancialOverviewDTO {
    private FinancialOverviewKPI kpi;
    private List<ExpenseTrendData> expenseTrend;
    private List<ExpenseCategoryData> expenseCategories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialOverviewKPI {
        private BigDecimal operatingExpenses;
        private BigDecimal inventoryPurchased;
        private BigDecimal grossProfit;
        private BigDecimal netProfit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseTrendData {
        private String period;
        private BigDecimal expenses;
        private BigDecimal purchases;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseCategoryData {
        private String name;
        private BigDecimal value;
        private String color;
    }
}
