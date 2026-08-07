package swari.sewa.module.finance.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FinanceExpensesResponse {
    private LocalDateTime generatedAt;
    private String currency;
    private DateRangeDTO dateRange;
    private FinanceExpensesKPI kpi;
    private List<ExpenseCategoryData> categoryBreakdown;
    private List<ExpenseTrendData> expenseTrend;

    @Data
    @Builder
    public static class DateRangeDTO {
        private LocalDateTime from;
        private LocalDateTime to;
    }

    @Data
    @Builder
    public static class FinanceExpensesKPI {
        private BigDecimal monthlyExpenses;
        private BigDecimal highestExpense;
        private String highestExpenseCategory;
    }

    @Data
    @Builder
    public static class ExpenseCategoryData {
        private String category;
        private BigDecimal amount;
        private BigDecimal percentage;
    }

    @Data
    @Builder
    public static class ExpenseTrendData {
        private String period;
        private BigDecimal amount;
    }
}
