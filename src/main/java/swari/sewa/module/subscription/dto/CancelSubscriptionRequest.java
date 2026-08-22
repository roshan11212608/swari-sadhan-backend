package swari.sewa.module.subscription.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CancelSubscriptionRequest {
    private String reason;
}
