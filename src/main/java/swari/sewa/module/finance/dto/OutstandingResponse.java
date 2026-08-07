package swari.sewa.module.finance.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OutstandingResponse {
    private LocalDateTime generatedAt;
    private String currency;
    private OutstandingKPI kpi;
    private List<ReceivableData> receivables;
    private List<PayableData> payables;

    @Data
    @Builder
    public static class OutstandingKPI {
        private BigDecimal totalReceivable;
        private BigDecimal totalPayable;
        private BigDecimal netOutstanding;
    }

    @Data
    @Builder
    public static class ReceivableData {
        private String type;
        private String name;
        private BigDecimal amount;
        private LocalDateTime dueDate;
    }

    @Data
    @Builder
    public static class PayableData {
        private String type;
        private String name;
        private BigDecimal amount;
        private LocalDateTime dueDate;
    }
}
