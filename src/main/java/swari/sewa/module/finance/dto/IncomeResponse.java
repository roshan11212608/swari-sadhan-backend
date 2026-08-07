package swari.sewa.module.finance.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class IncomeResponse {
    private LocalDateTime generatedAt;
    private String currency;
    private DateRangeDTO dateRange;
    private IncomeKPI kpi;
    private List<IncomeSourceData> incomeSources;
    private List<IncomeTrendData> incomeTrend;

    @Data
    @Builder
    public static class DateRangeDTO {
        private LocalDateTime from;
        private LocalDateTime to;
    }

    @Data
    @Builder
    public static class IncomeKPI {
        private BigDecimal todayIncome;
        private BigDecimal monthlyIncome;
        private BigDecimal yearlyIncome;
        private BigDecimal averageSale;
    }

    @Data
    @Builder
    public static class IncomeSourceData {
        private String source;
        private BigDecimal amount;
        private BigDecimal percentage;
    }

    @Data
    @Builder
    public static class IncomeTrendData {
        private String period;
        private BigDecimal amount;
    }
}
