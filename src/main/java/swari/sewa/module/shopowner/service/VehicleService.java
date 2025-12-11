package swari.sewa.module.shopowner.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import swari.sewa.module.shopowner.dto.VehicleDto;
import swari.sewa.common.dto.VehicleSearchRequest;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.VehicleType;

public interface VehicleService {
    
    VehicleDto createVehicle(VehicleDto vehicleDto, Long shopId);
    
    Optional<VehicleDto> getVehicleById(Long id);
    
    Page<VehicleDto> getAllVehicles(int page, int size);
    
    Page<VehicleDto> getVehiclesByShop(Long shopId, int page, int size);
    
    Page<VehicleDto> getVehiclesByCategory(Long categoryId, int page, int size);
    
    Page<VehicleDto> getVehiclesByType(VehicleType vehicleType, int page, int size);
    
    Page<VehicleDto> getActiveVehicles(int page, int size);
    
    Page<VehicleDto> getFeaturedVehicles(int page, int size);
    
    Page<VehicleDto> searchVehicles(VehicleSearchRequest searchRequest);
    
    VehicleDto updateVehicle(Long id, VehicleDto vehicleDto);
    
    void deleteVehicle(Long id);
    
    VehicleDto approveVehicle(Long id);
    
    VehicleDto rejectVehicle(Long id, String reason);
    
    VehicleDto activateVehicle(Long id);
    
    VehicleDto deactivateVehicle(Long id);
    
    VehicleDto markAsSold(Long id);
    
    VehicleDto incrementViewCount(Long id);
    
    VehicleDto incrementContactCount(Long id);
    
    List<VehicleDto> getPendingApprovalVehicles();
    
    List<VehicleDto> getVehiclesByStatus(VehicleStatus status);
    
    boolean existsByRegistrationNumber(String registrationNumber);
}
