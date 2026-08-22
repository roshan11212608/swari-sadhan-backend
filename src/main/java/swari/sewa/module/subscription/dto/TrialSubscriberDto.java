package swari.sewa.module.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrialSubscriberDto {
    private Long subscriptionId;
    private Long shopOwnerId;
    private String shopOwnerName;
    private String email;
    private String phone;
    private String shopName;
    private String shopAddress;
    private String planName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer daysRemaining;
    private Boolean active;
}
