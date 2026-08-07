package swari.sewa.module.finance.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentSummaryResponse {
    private LocalDateTime generatedAt;
    private String currency;
    private PaymentSummaryKPI kpi;

    @Data
    @Builder
    public static class PaymentSummaryKPI {
        private BigDecimal receivedToday;
        private BigDecimal receivedThisMonth;
        private BigDecimal pendingCustomerPayments;
        private BigDecimal supplierPayables;
        private BigDecimal financeCompanyReceivables;
    }
}
