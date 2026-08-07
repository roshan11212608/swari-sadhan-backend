package swari.sewa.module.finance.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CashFlowResponse {
    private LocalDateTime generatedAt;
    private String currency;
    private DateRangeDTO dateRange;
    private CashFlowKPI kpi;
    private List<MoneyInData> moneyIn;
    private List<MoneyOutData> moneyOut;
    private List<CashFlowTrendData> cashFlowTrend;

    @Data
    @Builder
    public static class DateRangeDTO {
        private LocalDateTime from;
        private LocalDateTime to;
    }

    @Data
    @Builder
    public static class CashFlowKPI {
        private BigDecimal totalMoneyIn;
        private BigDecimal totalMoneyOut;
        private BigDecimal netCashFlow;
    }

    @Data
    @Builder
    public static class MoneyInData {
        private String source;
        private BigDecimal amount;
    }

    @Data
    @Builder
    public static class MoneyOutData {
        private String category;
        private BigDecimal amount;
    }

    @Data
    @Builder
    public static class CashFlowTrendData {
        private String period;
        private BigDecimal moneyIn;
        private BigDecimal moneyOut;
        private BigDecimal netCashFlow;
    }
}
