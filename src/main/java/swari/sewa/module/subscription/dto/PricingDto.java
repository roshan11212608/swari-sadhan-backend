package swari.sewa.module.subscription.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PricingDto {
    @PositiveOrZero private BigDecimal monthly;
    @PositiveOrZero private BigDecimal quarterly;
    @PositiveOrZero private BigDecimal halfYearly;
    @PositiveOrZero private BigDecimal yearly;
    @NotBlank private String currency = "INR";
    private Boolean gstIncluded = true;
    @Min(0) @Max(100) private Integer discountPercentage = 0;
    @PositiveOrZero private BigDecimal strikePrice;
}
