package swari.sewa.module.subscription.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsageResponse {
    private Long id;
    private Long couponId;
    private Long transactionId;
    private Long shopOwnerId;
    private String shopOwnerName;
    private String shopOwnerEmail;
    private String shopOwnerPhone;
    private BigDecimal discountAmount;
    private LocalDateTime usedAt;
}
