package swari.sewa.module.superadmin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.shopowner.model.ShopOwner;
import swari.sewa.module.shopowner.repository.ShopOwnerRepository;
import swari.sewa.module.shopowner.model.Shop;
import swari.sewa.module.shopowner.repository.ShopRepository;
import swari.sewa.module.shopowner.model.Vehicle;
import swari.sewa.module.shopowner.repository.VehicleRepository;
import swari.sewa.module.superadmin.dto.ShopOwnerDto;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopOwnerServiceImpl implements ShopOwnerService {

    private final ShopOwnerRepository shopOwnerRepository;
    private final ShopRepository shopRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ShopOwnerDto> getAllShopOwners(Pageable pageable, String search, String status) {
        Page<ShopOwner> shopOwners;
        
        if (search != null && !search.trim().isEmpty()) {
            shopOwners = shopOwnerRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    search, search, search, pageable);
        } else if (status != null && !status.trim().isEmpty()) {
            boolean isActive = "active".equalsIgnoreCase(status);
            shopOwners = shopOwnerRepository.findByActive(isActive, pageable);
        } else {
            shopOwners = shopOwnerRepository.findAll(pageable);
        }
        
        return shopOwners.map(shopOwner -> ShopOwnerDto.builder()
                .id(shopOwner.getId())
                .firstName(shopOwner.getFirstName())
                .lastName(shopOwner.getLastName())
                .email(shopOwner.getEmail())
                .phone(shopOwner.getPhone())
                .companyName(shopOwner.getCompanyName())
                .active(shopOwner.isActive())
                .createdAt(shopOwner.getCreatedAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public ShopOwnerDto getShopOwnerById(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        
        return ShopOwnerDto.builder()
                .id(shopOwner.getId())
                .firstName(shopOwner.getFirstName())
                .lastName(shopOwner.getLastName())
                .email(shopOwner.getEmail())
                .phone(shopOwner.getPhone())
                .companyName(shopOwner.getCompanyName())
                .active(shopOwner.isActive())
                .createdAt(shopOwner.getCreatedAt())
                .build();
    }

    @Override
    public void approveShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(true);
        shopOwnerRepository.save(shopOwner);
    }

    @Override
    public void rejectShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(false);
        shopOwnerRepository.save(shopOwner);
    }

    @Override
    public void suspendShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(false);
        shopOwnerRepository.save(shopOwner);
    }

    @Override
    public void reactivateShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(true);
        shopOwnerRepository.save(shopOwner);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getShopOwnerShops(Long id, Pageable pageable) {
        return shopRepository.findByShopOwner_Id(id, pageable)
                .map(shop -> {
                    Map<String, Object> shopData = new HashMap<>();
                    shopData.put("id", shop.getId());
                    shopData.put("name", shop.getName());
                    shopData.put("status", shop.getStatus());
                    shopData.put("address", shop.getAddress());
                    shopData.put("createdAt", shop.getCreatedAt());
                    return shopData;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getShopOwnerVehicles(Long id, Pageable pageable) {
        return vehicleRepository.findByShop_ShopOwner_Id(id, pageable)
                .map(vehicle -> {
                    Map<String, Object> vehicleData = new HashMap<>();
                    vehicleData.put("id", vehicle.getId());
                    vehicleData.put("title", vehicle.getTitle());
                    vehicleData.put("status", vehicle.getStatus());
                    vehicleData.put("price", vehicle.getPrice());
                    vehicleData.put("views", vehicle.getViewCount());
                    vehicleData.put("createdAt", vehicle.getCreatedAt());
                    return vehicleData;
                });
    }
}
