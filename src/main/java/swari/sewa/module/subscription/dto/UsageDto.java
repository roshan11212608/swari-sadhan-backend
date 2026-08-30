package swari.sewa.module.subscription.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UsageDto {
    private Long vehiclesUsed;
    private Integer vehiclesLimit;
    private Long employeesUsed;
    private Integer employeesLimit;
    private String storageUsed;
}
