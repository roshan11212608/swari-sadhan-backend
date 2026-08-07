package swari.sewa.module.finance.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FinancialDashboardResponse {
    private LocalDateTime generatedAt;
    private String currency;
    private DateRangeDTO dateRange;
    private FinancialKPI kpi;
    private List<RevenueVsExpenseData> revenueVsExpenseTrend;
    private List<ProfitTrendData> profitTrend;
    private List<ExpenseCategoryData> expenseCategories;
    private List<YearlyOverviewData> yearlyOverview;
    private List<YearlyOverviewData> fiveYearOverview;

    @Data
    @Builder
    public static class DateRangeDTO {
        private LocalDateTime from;
        private LocalDateTime to;
    }

    @Data
    @Builder
    public static class FinancialKPI {
        private BigDecimal salesRevenue;
        private BigDecimal inventoryPurchase;
        private BigDecimal operatingExpenses;
        private BigDecimal grossProfit;
        private BigDecimal netProfit;
        private BigDecimal profitMargin;
        private BigDecimal cashAvailable;
    }

    @Data
    @Builder
    public static class RevenueVsExpenseData {
        private String period;
        private BigDecimal revenue;
        private BigDecimal expenses;
    }

    @Data
    @Builder
    public static class ProfitTrendData {
        private String period;
        private BigDecimal grossProfit;
    }

    @Data
    @Builder
    public static class ExpenseCategoryData {
        private String name;
        private BigDecimal value;
        private String color;
    }

    @Data
    @Builder
    public static class YearlyOverviewData {
        private String month;
        private BigDecimal revenue;
        private BigDecimal expenses;
        private BigDecimal profit;
    }
}
