package swari.sewa.module.subscription.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateSubscriptionPlanRequest {
    @NotBlank @Size(max = 100) private String name;
    private String description;
    @Size(max = 255) private String shortDescription;
    @NotBlank private String category; // BASIC, STANDARD, PREMIUM, ULTIMATE, CUSTOM
    private String icon;
    private String themeColor;
    private Integer sortOrder;
    private Boolean isPopular;
    private Boolean isRecommended;
    private String visibility; // PUBLIC, PRIVATE
    @NotNull private PricingDto pricing;
    @NotNull private RestrictionDto restrictions;
    private List<FeatureDto> features;
    @NotNull @Positive private Integer durationDays;
    private Boolean autoRenewal;
}
