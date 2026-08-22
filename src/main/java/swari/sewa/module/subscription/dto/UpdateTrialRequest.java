package swari.sewa.module.subscription.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateTrialRequest {
    @NotBlank private String name;
    private String description;
    @Positive private Integer duration;
    private Integer vehicleLimit;
    private Long trialPlanId;
    private Boolean active;
}
