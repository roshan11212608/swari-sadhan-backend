package swari.sewa.module.subscription.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TrialResponse {
    private Long id;
    private String name;
    private String description;
    private Integer duration;
    private Integer vehicleLimit;
    private String eligibilityRules;
    private Long trialPlanId;
    private String trialPlanName;
    private Boolean active;
    private Long activeUsers;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
