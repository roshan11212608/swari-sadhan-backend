package swari.sewa.module.vehicle.service;

import swari.sewa.module.vehicle.dto.SellApplicationDto;
import swari.sewa.module.vehicle.dto.SellVehicleApplicationDto;

import java.util.List;

public interface SellApplicationService {
    SellApplicationDto createSellApplication(SellVehicleApplicationDto sellApplicationDto);
    List<SellApplicationDto> getSellApplicationsByVehicleId(Long vehicleId);
}
