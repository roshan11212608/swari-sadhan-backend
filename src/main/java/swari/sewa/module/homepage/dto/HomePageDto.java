package swari.sewa.module.homepage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomePageDto {
    private List<HomeBrandDto> brands;
    private List<HomeFeaturedVehicleDto> featuredVehicles;
    private List<HomeBudgetDto> budgets;
    private List<HomeServiceDto> services;
    private List<HomeStoreDto> stores;
}
