package swari.sewa.module.subscription.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RestrictionDto {
    @PositiveOrZero private Integer maxVehicles;
    @PositiveOrZero private Integer maxEmployees;
    private String maxStorage;
    @PositiveOrZero private Integer maxBranches;
    @PositiveOrZero private Integer apiCalls;
    private String supportLevel;
    @PositiveOrZero private Integer dailyUploadLimit;
    private String backupFrequency;
}
