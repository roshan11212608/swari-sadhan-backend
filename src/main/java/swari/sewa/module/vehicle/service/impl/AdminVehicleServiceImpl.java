package swari.sewa.module.vehicle.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.VehicleType;
import swari.sewa.module.vehicle.service.AdminVehicleService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminVehicleServiceImpl implements AdminVehicleService {

    private final VehicleRepository vehicleRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getAllVehicles(Pageable pageable, String search, String status, String type) {
        Page<Vehicle> vehicles;

        if (search != null && !search.trim().isEmpty()) {
            vehicles = vehicleRepository.searchByKeywordWithShop(search, pageable);
        } else if (status != null && !status.trim().isEmpty()) {
            VehicleStatus vehicleStatus = VehicleStatus.valueOf(status.toUpperCase());
            vehicles = vehicleRepository.findByStatusWithShop(vehicleStatus, pageable);
        } else if (type != null && !type.trim().isEmpty()) {
            VehicleType vehicleType = VehicleType.valueOf(type.toUpperCase());
            vehicles = vehicleRepository.findByTypeWithShop(vehicleType, pageable);
        } else {
            vehicles = vehicleRepository.findAllWithShop(pageable);
        }
        
        return vehicles.map(vehicle -> {
            Map<String, Object> vehicleData = new HashMap<>();
            vehicleData.put("id", vehicle.getId());
            vehicleData.put("title", vehicle.getTitle());
            vehicleData.put("brandName", vehicle.getBrandName());
            vehicleData.put("modelName", vehicle.getModelName());
            vehicleData.put("kilometersDriven", vehicle.getKilometersDriven());
            vehicleData.put("lotsNumber", vehicle.getLotsNumber());
            vehicleData.put("status", vehicle.getStatus());
            vehicleData.put("type", vehicle.getType());
            vehicleData.put("purchasePrice", vehicle.getPurchasePrice());
            vehicleData.put("price", vehicle.getPurchasePrice());
            vehicleData.put("sellPrice", getActualSellPrice(vehicle));
            vehicleData.put("views", vehicle.getViewCount());
            vehicleData.put("contacts", vehicle.getContactCount());
            vehicleData.put("createdAt", vehicle.getCreatedAt());
            if (vehicle.getShop() != null) {
                vehicleData.put("shopName", vehicle.getShop().getName());
                vehicleData.put("shopAddress", vehicle.getShop().getAddress());
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
        vehicleData.put("purchasePrice", vehicle.getPurchasePrice());
        vehicleData.put("sellPrice", getActualSellPrice(vehicle));
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
        return vehicleRepository.findByStatusWithShop(VehicleStatus.PENDING_APPROVAL, pageable)
                .map(vehicle -> {
                    Map<String, Object> vehicleData = new HashMap<>();
                    vehicleData.put("id", vehicle.getId());
                    vehicleData.put("title", vehicle.getTitle());
                    vehicleData.put("price", vehicle.getPrice());
                    vehicleData.put("sellPrice", getActualSellPrice(vehicle));
                    vehicleData.put("purchasePrice", vehicle.getPurchasePrice());
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
                    vehicleData.put("price", vehicle.getSellingPrice());
                    vehicleData.put("purchasePrice", vehicle.getPurchasePrice());
                    vehicleData.put("sellPrice", vehicle.getSellingPrice());
                    vehicleData.put("views", vehicle.getViewCount());
                    vehicleData.put("mainImageUrl", vehicle.getMainImageUrl());
                    if (vehicle.getShop() != null) {
                        vehicleData.put("shopName", vehicle.getShop().getName());
                    }
                    return vehicleData;
                });
    }

    private BigDecimal getActualSellPrice(Vehicle vehicle) {
        if (vehicle.getSellingPrice() != null) {
            return vehicle.getSellingPrice();
        }
        BigDecimal sellPrice = null;
        if (vehicle.getSpecifications() != null && !vehicle.getSpecifications().isEmpty()) {
            try {
                JsonNode specs = objectMapper.readTree(vehicle.getSpecifications());
                JsonNode specSellPrice = specs.get("sellPrice");
                if (specSellPrice != null && !specSellPrice.isNull()) {
                    String raw = specSellPrice.asText().replaceAll("[^\\d.]", "");
                    if (!raw.isEmpty()) {
                        sellPrice = new BigDecimal(raw);
                    }
                }
            } catch (Exception e) {
                // Ignore parse errors
            }
        }
        return sellPrice;
    }
}
