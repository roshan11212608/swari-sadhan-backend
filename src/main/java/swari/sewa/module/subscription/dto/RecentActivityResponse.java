package swari.sewa.module.subscription.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecentActivityResponse {
    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private Long adminUserId;
    private String description;
    private String status;
    private LocalDateTime createdDate;
}
