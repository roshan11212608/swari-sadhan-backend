package swari.sewa.module.finance.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProfitResponse {
    private LocalDateTime generatedAt;
    private String currency;
    private DateRangeDTO dateRange;
    private ProfitBreakdown profitBreakdown;
    private List<ProfitTrendData> profitTrend;

    @Data
    @Builder
    public static class DateRangeDTO {
        private LocalDateTime from;
        private LocalDateTime to;
    }

    @Data
    @Builder
    public static class ProfitBreakdown {
        private BigDecimal revenue;
        private BigDecimal cogs;
        private BigDecimal grossProfit;
        private BigDecimal operatingExpenses;
        private BigDecimal netProfit;
        private BigDecimal profitMargin;
    }

    @Data
    @Builder
    public static class ProfitTrendData {
        private String period;
        private BigDecimal grossProfit;
        private BigDecimal netProfit;
    }
}
