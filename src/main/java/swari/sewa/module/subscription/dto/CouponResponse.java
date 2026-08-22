package swari.sewa.module.subscription.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CouponResponse {
    private Long id;
    private String code;
    private String discountType;
    private Integer percentage;
    private BigDecimal flatDiscount;
    private BigDecimal maximumDiscount;
    private BigDecimal minimumPurchase;
    private Integer usageLimit;
    private LocalDate expiryDate;
    private Boolean active;
    private Integer usedCount;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
