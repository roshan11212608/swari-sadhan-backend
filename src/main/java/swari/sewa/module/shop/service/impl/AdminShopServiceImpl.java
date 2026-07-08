package swari.sewa.module.shop.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.common.enums.ShopStatus;
import swari.sewa.module.shop.service.AdminShopService;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminShopServiceImpl implements AdminShopService {

    private final ShopRepository shopRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getAllShops(Pageable pageable, String search, String status) {
        Page<Shop> shops;
        
        if (search != null && !search.trim().isEmpty()) {
            shops = shopRepository.findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(
                    search, search, pageable);
        } else if (status != null && !status.trim().isEmpty()) {
            ShopStatus shopStatus = ShopStatus.valueOf(status.toUpperCase());
            shops = shopRepository.findByStatus(shopStatus, pageable);
        } else {
            shops = shopRepository.findAll(pageable);
        }
        
        return shops.map(shop -> {
            Map<String, Object> shopData = new HashMap<>();
            shopData.put("id", shop.getId());
            shopData.put("name", shop.getName());
            shopData.put("status", shop.getStatus());
            shopData.put("address", shop.getAddress());
            shopData.put("phone", shop.getPhone());
            shopData.put("email", shop.getEmail());
            shopData.put("licenseNumber", shop.getLicenseNumber());
            shopData.put("createdAt", shop.getCreatedAt());
            if (shop.getShopOwner() != null) {
                shopData.put("ownerName", shop.getShopOwner().getFirstName() + " " + shop.getShopOwner().getLastName());
            }
            return shopData;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Object getShopById(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found"));
        
        Map<String, Object> shopData = new HashMap<>();
        shopData.put("id", shop.getId());
        shopData.put("name", shop.getName());
        shopData.put("status", shop.getStatus());
        shopData.put("address", shop.getAddress());
        shopData.put("phone", shop.getPhone());
        shopData.put("email", shop.getEmail());
        shopData.put("licenseNumber", shop.getLicenseNumber());
        shopData.put("description", shop.getDescription());
        shopData.put("createdAt", shop.getCreatedAt());
        if (shop.getShopOwner() != null) {
            shopData.put("ownerName", shop.getShopOwner().getFirstName() + " " + shop.getShopOwner().getLastName());
            shopData.put("ownerEmail", shop.getShopOwner().getEmail());
        }
        return shopData;
    }

    @Override
    public void approveShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found"));
        shop.setStatus(ShopStatus.ACTIVE);
        shopRepository.save(shop);
    }

    @Override
    public void rejectShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found"));
        shop.setStatus(ShopStatus.REJECTED);
        shopRepository.save(shop);
    }

    @Override
    public void suspendShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found"));
        shop.setStatus(ShopStatus.SUSPENDED);
        shopRepository.save(shop);
    }

    @Override
    public void reactivateShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found"));
        shop.setStatus(ShopStatus.ACTIVE);
        shopRepository.save(shop);
    }

    @Override
    public void deleteShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found"));
        shopRepository.delete(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getPendingShops(Pageable pageable) {
        return shopRepository.findByStatus(ShopStatus.PENDING_APPROVAL, pageable)
                .map(shop -> {
                    Map<String, Object> shopData = new HashMap<>();
                    shopData.put("id", shop.getId());
                    shopData.put("name", shop.getName());
                    shopData.put("address", shop.getAddress());
                    shopData.put("phone", shop.getPhone());
                    shopData.put("email", shop.getEmail());
                    shopData.put("licenseNumber", shop.getLicenseNumber());
                    shopData.put("createdAt", shop.getCreatedAt());
                    if (shop.getShopOwner() != null) {
                        shopData.put("ownerName", shop.getShopOwner().getFirstName() + " " + shop.getShopOwner().getLastName());
                        shopData.put("ownerEmail", shop.getShopOwner().getEmail());
                    }
                    return shopData;
                });
    }
}
