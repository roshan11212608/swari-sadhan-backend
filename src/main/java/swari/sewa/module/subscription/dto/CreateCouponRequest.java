package swari.sewa.module.subscription.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateCouponRequest {
    @NotBlank @Size(max = 50) private String code;
    @NotBlank private String discountType; // PERCENTAGE or FLAT
    @Min(1) @Max(100) private Integer percentage;
    @Positive private BigDecimal flatDiscount;
    @PositiveOrZero private BigDecimal maximumDiscount;
    @PositiveOrZero private BigDecimal minimumPurchase;
    @Positive private Integer usageLimit;
    private LocalDate expiryDate;
    private Boolean active;
}
