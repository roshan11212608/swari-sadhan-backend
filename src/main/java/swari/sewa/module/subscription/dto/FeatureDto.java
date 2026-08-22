package swari.sewa.module.subscription.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FeatureDto {
    private Long id;
    @NotBlank private String name;
    private String icon;
    private String description;
    private Boolean included = false;
    @PositiveOrZero private Integer limit;
}
