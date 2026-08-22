package swari.sewa.module.subscription.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UsageDto {
    private Long vehiclesUsed;
    private Long employeesUsed;
    private String storageUsed;
}
