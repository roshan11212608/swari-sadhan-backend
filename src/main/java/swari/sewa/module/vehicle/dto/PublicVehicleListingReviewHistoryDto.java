package swari.sewa.module.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicVehicleListingReviewHistoryDto {
    private Long id;
    private String actor;
    private String action;
    private String reason;
    private String notes;
    private LocalDateTime performedAt;
}
