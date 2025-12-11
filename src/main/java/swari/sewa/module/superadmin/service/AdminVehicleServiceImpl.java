package swari.sewa.module.superadmin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.shopowner.model.Vehicle;
import swari.sewa.module.shopowner.repository.VehicleRepository;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.VehicleType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminVehicleServiceImpl implements AdminVehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getAllVehicles(Pageable pageable, String search, String status, String type) {
        Page<Vehicle> vehicles;
        
        if (search != null && !search.trim().isEmpty()) {
            vehicles = vehicleRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    search, search, pageable);
        } else if (status != null && !status.trim().isEmpty()) {
            VehicleStatus vehicleStatus = VehicleStatus.valueOf(status.toUpperCase());
            vehicles = vehicleRepository.findByStatus(vehicleStatus, pageable);
        } else if (type != null && !type.trim().isEmpty()) {
            VehicleType vehicleType = VehicleType.valueOf(type.toUpperCase());
            vehicles = vehicleRepository.findByType(vehicleType, pageable);
        } else {
            vehicles = vehicleRepository.findAll(pageable);
        }
        
        return vehicles.map(vehicle -> {
            Map<String, Object> vehicleData = new HashMap<>();
            vehicleData.put("id", vehicle.getId());
            vehicleData.put("title", vehicle.getTitle());
            vehicleData.put("status", vehicle.getStatus());
            vehicleData.put("type", vehicle.getType());
            vehicleData.put("price", vehicle.getPrice());
            vehicleData.put("views", vehicle.getViewCount());
            vehicleData.put("contacts", vehicle.getContactCount());
            vehicleData.put("createdAt", vehicle.getCreatedAt());
            if (vehicle.getShop() != null) {
                vehicleData.put("shopName", vehicle.getShop().getName());
            }
            return vehicleData;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Object getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        
        Map<String, Object> vehicleData = new HashMap<>();
        vehicleData.put("id", vehicle.getId());
        vehicleData.put("title", vehicle.getTitle());
        vehicleData.put("description", vehicle.getDescription());
        vehicleData.put("status", vehicle.getStatus());
        vehicleData.put("type", vehicle.getType());
        vehicleData.put("price", vehicle.getPrice());
        vehicleData.put("views", vehicle.getViewCount());
        vehicleData.put("contacts", vehicle.getContactCount());
        vehicleData.put("registrationNumber", vehicle.getRegistrationNumber());
        vehicleData.put("year", vehicle.getYear());
        vehicleData.put("mileage", vehicle.getMileage());
        vehicleData.put("mainImageUrl", vehicle.getMainImageUrl());
        vehicleData.put("createdAt", vehicle.getCreatedAt());
        vehicleData.put("updatedAt", vehicle.getUpdatedAt());
        
        if (vehicle.getShop() != null) {
            vehicleData.put("shopName", vehicle.getShop().getName());
            vehicleData.put("shopId", vehicle.getShop().getId());
        }
        
        if (vehicle.getCategory() != null) {
            vehicleData.put("categoryName", vehicle.getCategory().getName());
            vehicleData.put("categoryId", vehicle.getCategory().getId());
        }
        
        return vehicleData;
    }

    @Override
    public void approveVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicleRepository.save(vehicle);
    }

    @Override
    public void rejectVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicle.setStatus(VehicleStatus.REJECTED);
        vehicleRepository.save(vehicle);
    }

    @Override
    public void suspendVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicle.setStatus(VehicleStatus.SUSPENDED);
        vehicleRepository.save(vehicle);
    }

    @Override
    public void reactivateVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicleRepository.save(vehicle);
    }

    @Override
    public void markVehicleAsSold(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicle.setStatus(VehicleStatus.SOLD);
        vehicle.setSoldAt(LocalDateTime.now());
        vehicleRepository.save(vehicle);
    }

    @Override
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicleRepository.delete(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getPendingVehicles(Pageable pageable) {
        return vehicleRepository.findByStatus(VehicleStatus.PENDING_APPROVAL, pageable)
                .map(vehicle -> {
                    Map<String, Object> vehicleData = new HashMap<>();
                    vehicleData.put("id", vehicle.getId());
                    vehicleData.put("title", vehicle.getTitle());
                    vehicleData.put("price", vehicle.getPrice());
                    vehicleData.put("year", vehicle.getYear());
                    vehicleData.put("mileage", vehicle.getMileage());
                    vehicleData.put("mainImageUrl", vehicle.getMainImageUrl());
                    vehicleData.put("createdAt", vehicle.getCreatedAt());
                    if (vehicle.getShop() != null) {
                        vehicleData.put("shopName", vehicle.getShop().getName());
                    }
                    return vehicleData;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getFeaturedVehicles(Pageable pageable) {
        return vehicleRepository.findByIsFeaturedTrue(pageable)
                .map(vehicle -> {
                    Map<String, Object> vehicleData = new HashMap<>();
                    vehicleData.put("id", vehicle.getId());
                    vehicleData.put("title", vehicle.getTitle());
                    vehicleData.put("price", vehicle.getPrice());
                    vehicleData.put("views", vehicle.getViewCount());
                    vehicleData.put("mainImageUrl", vehicle.getMainImageUrl());
                    if (vehicle.getShop() != null) {
                        vehicleData.put("shopName", vehicle.getShop().getName());
                    }
                    return vehicleData;
                });
    }
}
