package swari.sewa.module.subscription.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DowngradeSubscriptionRequest {
    @NotNull @Positive private Long targetPlanId;
    private String reason;
}
