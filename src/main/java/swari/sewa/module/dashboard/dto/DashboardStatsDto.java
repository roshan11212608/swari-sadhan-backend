package swari.sewa.module.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private Long totalShopOwners;
    private Long activeShopOwners;
    private Long totalShops;
    private Long activeShops;
    private Long totalVehicles;
    private Long activeVehicles;
    private Long totalUsers;
    private Long totalEnquiries;
    private Long pendingEnquiries;
}
