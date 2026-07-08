package swari.sewa.module.vehicle.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminVehicleService {
    
    Page<Object> getAllVehicles(Pageable pageable, String search, String status, String type);
    
    Object getVehicleById(Long id);
    
    void approveVehicle(Long id);
    
    void rejectVehicle(Long id);
    
    void suspendVehicle(Long id);
    
    void reactivateVehicle(Long id);
    
    void markVehicleAsSold(Long id);
    
    void deleteVehicle(Long id);
    
    Page<Object> getPendingVehicles(Pageable pageable);
    
    Page<Object> getFeaturedVehicles(Pageable pageable);
}
