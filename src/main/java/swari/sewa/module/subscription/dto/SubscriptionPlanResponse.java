package swari.sewa.module.subscription.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubscriptionPlanResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String shortDescription;
    private String category;
    private String icon;
    private String themeColor;
    private Integer sortOrder;
    private Boolean isPopular;
    private Boolean isRecommended;
    private String visibility;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private PricingDto pricing;
    private RestrictionDto restrictions;
    private List<FeatureDto> features;
    private Long subscriberCount;
}
