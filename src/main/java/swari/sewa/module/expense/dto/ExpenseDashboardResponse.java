package swari.sewa.module.expense.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDashboardResponse {
    private SummaryMetrics summary;
    private List<MonthlyTrend> monthlyTrend;
    private MonthlyComparison monthlyComparison;
    private List<PaymentMethodDistribution> paymentMethodDistribution;
    private List<ExpenseResponse> recentExpenses;
    private List<ExpenseResponse> upcomingPayments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryMetrics {
        private BigDecimal periodExpense;
        private BigDecimal pendingPayments;
        private BigDecimal paidExpenses;
        private BigDecimal averageDailyExpense;
        private Long totalExpenses;
        private String periodLabel;
        private BigDecimal todayExpense;
        private BigDecimal yesterdayExpense;
        private BigDecimal weekExpense;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrend {
        private String month;
        private String year;
        private BigDecimal amount;
        private Long expenseCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyComparison {
        private BigDecimal currentPeriod;
        private BigDecimal previousPeriod;
        private BigDecimal difference;
        private BigDecimal percentageChange;
        private String trend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodDistribution {
        private String paymentMethod;
        private BigDecimal amount;
        private BigDecimal percentage;
        private Long count;
    }
}
