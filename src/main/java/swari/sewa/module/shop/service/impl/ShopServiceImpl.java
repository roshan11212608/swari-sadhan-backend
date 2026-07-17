package swari.sewa.module.shop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.shop.dto.ShopDto;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.common.exception.LicenseNumberAlreadyExistsException;
import swari.sewa.common.exception.ShopServiceException;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.shop.service.ShopService;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.common.enums.ShopStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final VehicleRepository vehicleRepository;
    private final ModelMapper modelMapper;

    private ShopDto mapToDto(Shop shop, java.util.Map<Long, Long> vehicleCountMap) {
        ShopDto dto = new ShopDto();
        dto.setId(shop.getId());
        dto.setName(shop.getName());
        dto.setDescription(shop.getDescription());
        dto.setLicenseNumber(shop.getLicenseNumber());
        dto.setAddressLine1(shop.getAddressLine1());
        dto.setAddressLine2(shop.getAddressLine2());
        dto.setCity(shop.getCity());
        dto.setState(shop.getState());
        dto.setCountry(shop.getCountry());
        dto.setPostalCode(shop.getPostalCode());
        dto.setPhoneNumber(shop.getPhoneNumber());
        dto.setEmailAddress(shop.getEmailAddress());
        dto.setWebsiteUrl(shop.getWebsiteUrl());
        dto.setLatitude(shop.getLatitude());
        dto.setLongitude(shop.getLongitude());
        dto.setLogoUrl(shop.getLogoUrl());
        dto.setOpeningHours(shop.getOpeningHours());
        dto.setStatus(shop.getStatus());
        dto.setIsFeatured(shop.getIsFeatured());
        dto.setSubscriptionPlan(shop.getSubscriptionPlan());
        dto.setSubscriptionExpiry(shop.getSubscriptionExpiry());
        dto.setCreatedAt(shop.getCreatedAt());
        dto.setUpdatedAt(shop.getUpdatedAt());

        // Frontend-friendly aliases
        dto.setShopName(shop.getName());
        dto.setShopPhone(shop.getPhoneNumber());
        dto.setShopEmail(shop.getEmailAddress());
        dto.setLocation(shop.getCity());

        if (shop.getShopOwner() != null) {
            ShopOwner owner = shop.getShopOwner();
            dto.setShopOwnerId(owner.getId());
            String firstName = owner.getFirstName() != null ? owner.getFirstName() : "";
            String lastName = owner.getLastName() != null ? owner.getLastName() : "";
            String ownerName = (firstName + " " + lastName).trim();
            dto.setOwnerName(ownerName.isEmpty() ? "Owner" : ownerName);
            dto.setOwnerEmail(owner.getEmail());
            dto.setOwnerPhone(owner.getPhone());
            dto.setKycStatus(owner.getKycVerified() != null && owner.getKycVerified() ? "verified" : "pending");

            // Use shop owner's shop logo as fallback if shop logo is not set
            String logoUrl = shop.getLogoUrl();
            if (logoUrl == null && owner.getShopLogo() != null) {
                logoUrl = owner.getShopLogo();
            }
            dto.setLogoUrl(logoUrl);
            dto.setLogo(logoUrl);

            // Build location from shop owner's detailed address fields
            java.util.List<String> locationParts = new java.util.ArrayList<>();
            if (owner.getTole() != null && !owner.getTole().isEmpty()) {
                locationParts.add(owner.getTole());
            }
            if (owner.getMunicipality() != null && !owner.getMunicipality().isEmpty()) {
                locationParts.add(owner.getMunicipality());
            }
            if (owner.getDistrict() != null && !owner.getDistrict().isEmpty()) {
                locationParts.add(owner.getDistrict());
            }
            if (owner.getProvince() != null && !owner.getProvince().isEmpty()) {
                locationParts.add(owner.getProvince());
            }
            // Fallback to shop's city/state if owner location is empty
            if (locationParts.isEmpty()) {
                if (shop.getCity() != null && !shop.getCity().isEmpty()) {
                    locationParts.add(shop.getCity());
                }
                if (shop.getState() != null && !shop.getState().isEmpty()) {
                    locationParts.add(shop.getState());
                }
            }
            String location = String.join(", ", locationParts);
            dto.setLocation(location.isEmpty() ? shop.getCity() : location);
        } else {
            dto.setShopOwnerId(null);
            dto.setOwnerName("Owner");
            dto.setOwnerEmail(null);
            dto.setOwnerPhone(null);
            dto.setKycStatus("pending");
            dto.setLogoUrl(shop.getLogoUrl());
            dto.setLogo(shop.getLogoUrl());
            dto.setLocation(shop.getCity());
        }

        // Use pre-fetched vehicle count map instead of querying database
        Long vehicleCount = vehicleCountMap.getOrDefault(shop.getId(), 0L);
        dto.setVehicleCount(vehicleCount.intValue());
        dto.setTotalVehicles(vehicleCount.intValue());

        // Static rating/review placeholders; replace with real aggregates when available
        dto.setRating(4.5);
        dto.setReviewCount(0);
        dto.setTotalReviews(0);

        return dto;
    }

    // Overloaded method for backward compatibility (used in single-shop operations)
    private ShopDto mapToDto(Shop shop) {
        return mapToDto(shop, java.util.Collections.singletonMap(
            shop.getId(),
            vehicleRepository.countByShopId(shop.getId())
        ));
    }

    @Override
    public ShopDto createShop(ShopDto shopDto, Long userId) {
        if (shopRepository.existsByLicenseNumber(shopDto.getLicenseNumber())) {
            throw new LicenseNumberAlreadyExistsException("License number already exists: " + shopDto.getLicenseNumber());
        }

        ShopOwner shopOwner = shopOwnerRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ShopOwner not found with id: " + userId));

        Shop shop = modelMapper.map(shopDto, Shop.class);
        shop.setShopOwner(shopOwner);
        shop.setStatus(ShopStatus.PENDING_APPROVAL);

        // Copy shop logo from shop owner if not already set
        if (shop.getLogoUrl() == null && shopOwner.getShopLogo() != null) {
            shop.setLogoUrl(shopOwner.getShopLogo());
        }

        Shop savedShop = shopRepository.save(shop);
        return mapToDto(savedShop);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShopDto> getShopById(Long id) {
        return shopRepository.findByIdWithShopOwner(id).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShopDto> getShopByUserId(Long userId) {
        return shopRepository.findByShopOwnerIdWithShopOwner(userId).stream().findFirst().map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> getAllShops() {
        try {
            // Fetch all shops with shopOwner in one query
            List<Shop> shops = shopRepository.findAllWithShopOwner();
            
            // Fetch all vehicle counts in one aggregation query
            java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMap();
            
            // Map to DTOs using pre-fetched counts
            return shops.stream()
                    .map(shop -> mapToDto(shop, vehicleCountMap))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching all shops: {}", e.getMessage(), e);
            throw new ShopServiceException("Failed to fetch shops", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ShopDto> getAllShops(Pageable pageable) {
        try {
            // Fetch paginated shops with shopOwner in one query
            Page<Shop> shopsPage = shopRepository.findAllWithShopOwner(pageable);
            
            // Fetch vehicle counts only for shops in current page
            java.util.List<Long> shopIds = shopsPage.getContent().stream()
                    .map(Shop::getId)
                    .collect(Collectors.toList());
            java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
            
            // Map to DTOs using pre-fetched counts
            java.util.List<ShopDto> dtoList = shopsPage.getContent().stream()
                    .map(shop -> mapToDto(shop, vehicleCountMap))
                    .collect(Collectors.toList());
            
            return new org.springframework.data.domain.PageImpl<>(dtoList, pageable, shopsPage.getTotalElements());
        } catch (Exception e) {
            log.error("Error fetching paginated shops: {}", e.getMessage(), e);
            throw new ShopServiceException("Failed to fetch paginated shops", e);
        }
    }
    
    /**
     * Builds a Map of shopId -> vehicleCount using a single aggregation query.
     * This eliminates N+1 query problem when fetching multiple shops.
     */
    private java.util.Map<Long, Long> buildVehicleCountMap() {
        java.util.List<java.util.Map<String, Object>> results = vehicleRepository.countVehiclesByShopGrouped();
        java.util.Map<Long, Long> countMap = new java.util.HashMap<>();
        
        for (java.util.Map<String, Object> result : results) {
            Long shopId = (Long) result.get("shopId");
            Long count = (Long) result.get("count");
            if (shopId != null && count != null) {
                countMap.put(shopId, count);
            }
        }
        
        return countMap;
    }
    
    /**
     * Builds a Map of shopId -> vehicleCount for specific shop IDs only.
     * More efficient than fetching all counts when paginating.
     */
    private java.util.Map<Long, Long> buildVehicleCountMapForShopIds(java.util.List<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return new java.util.HashMap<>();
        }
        
        java.util.Map<Long, Long> countMap = new java.util.HashMap<>();
        for (Long shopId : shopIds) {
            Long count = vehicleRepository.countByShopId(shopId);
            countMap.put(shopId, count != null ? count : 0L);
        }
        
        return countMap;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> getShopsByCity(String city) {
        return shopRepository.findByCityAndStatusActiveWithShopOwner(city).stream().map(this::mapToDto).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ShopDto> getShopsByCity(String city, Pageable pageable) {
        try {
            Page<Shop> shopsPage = shopRepository.findByCityAndStatusActiveWithShopOwner(city, pageable);
            java.util.List<Long> shopIds = shopsPage.getContent().stream().map(Shop::getId).collect(Collectors.toList());
            java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
            
            java.util.List<ShopDto> dtoList = shopsPage.getContent().stream()
                    .map(shop -> mapToDto(shop, vehicleCountMap))
                    .collect(Collectors.toList());
            
            return new org.springframework.data.domain.PageImpl<>(dtoList, pageable, shopsPage.getTotalElements());
        } catch (Exception e) {
            log.error("Error fetching paginated shops by city: {}", e.getMessage(), e);
            throw new ShopServiceException("Failed to fetch shops by city", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> getShopsByState(String state) {
        return shopRepository.findByStateAndStatusActiveWithShopOwner(state).stream().map(this::mapToDto).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ShopDto> getShopsByState(String state, Pageable pageable) {
        try {
            Page<Shop> shopsPage = shopRepository.findByStateAndStatusActiveWithShopOwner(state, pageable);
            java.util.List<Long> shopIds = shopsPage.getContent().stream().map(Shop::getId).collect(Collectors.toList());
            java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
            
            java.util.List<ShopDto> dtoList = shopsPage.getContent().stream()
                    .map(shop -> mapToDto(shop, vehicleCountMap))
                    .collect(Collectors.toList());
            
            return new org.springframework.data.domain.PageImpl<>(dtoList, pageable, shopsPage.getTotalElements());
        } catch (Exception e) {
            log.error("Error fetching paginated shops by state: {}", e.getMessage(), e);
            throw new ShopServiceException("Failed to fetch shops by state", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> getFeaturedShops() {
        return shopRepository.findFeaturedShopsWithShopOwner().stream().map(this::mapToDto).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ShopDto> getFeaturedShops(Pageable pageable) {
        try {
            Page<Shop> shopsPage = shopRepository.findFeaturedShopsWithShopOwner(pageable);
            java.util.List<Long> shopIds = shopsPage.getContent().stream().map(Shop::getId).collect(Collectors.toList());
            java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
            
            java.util.List<ShopDto> dtoList = shopsPage.getContent().stream()
                    .map(shop -> mapToDto(shop, vehicleCountMap))
                    .collect(Collectors.toList());
            
            return new org.springframework.data.domain.PageImpl<>(dtoList, pageable, shopsPage.getTotalElements());
        } catch (Exception e) {
            log.error("Error fetching paginated featured shops: {}", e.getMessage(), e);
            throw new ShopServiceException("Failed to fetch featured shops", e);
        }
    }

    @Override
    public ShopDto updateShop(Long id, ShopDto shopDto) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));

        if (!shop.getLicenseNumber().equals(shopDto.getLicenseNumber()) && 
            shopRepository.existsByLicenseNumber(shopDto.getLicenseNumber())) {
            throw new LicenseNumberAlreadyExistsException("License number already exists: " + shopDto.getLicenseNumber());
        }

        modelMapper.map(shopDto, shop);
        Shop updatedShop = shopRepository.save(shop);
        return mapToDto(updatedShop);
    }

    @Override
    public void deleteShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        shopRepository.delete(shop);
    }

    @Override
    public ShopDto approveShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        shop.setStatus(ShopStatus.ACTIVE);
        return mapToDto(shopRepository.save(shop));
    }

    @Override
    public ShopDto rejectShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        shop.setStatus(ShopStatus.REJECTED);
        return mapToDto(shopRepository.save(shop));
    }

    @Override
    public ShopDto activateShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        shop.setStatus(ShopStatus.ACTIVE);
        return mapToDto(shopRepository.save(shop));
    }

    @Override
    public ShopDto deactivateShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        shop.setStatus(ShopStatus.INACTIVE);
        return mapToDto(shopRepository.save(shop));
    }

    @Override
    public ShopDto suspendShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        shop.setStatus(ShopStatus.SUSPENDED);
        return mapToDto(shopRepository.save(shop));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByLicenseNumber(String licenseNumber) {
        return shopRepository.existsByLicenseNumber(licenseNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> searchShops(String keyword) {
        return shopRepository.searchByKeywordWithShopOwner(keyword).stream().map(this::mapToDto).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ShopDto> searchShops(String keyword, Pageable pageable) {
        try {
            Page<Shop> shopsPage = shopRepository.searchByKeywordWithShopOwner(keyword, pageable);
            java.util.List<Long> shopIds = shopsPage.getContent().stream().map(Shop::getId).collect(Collectors.toList());
            java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
            
            java.util.List<ShopDto> dtoList = shopsPage.getContent().stream()
                    .map(shop -> mapToDto(shop, vehicleCountMap))
                    .collect(Collectors.toList());
            
            return new org.springframework.data.domain.PageImpl<>(dtoList, pageable, shopsPage.getTotalElements());
        } catch (Exception e) {
            log.error("Error searching paginated shops: {}", e.getMessage(), e);
            throw new ShopServiceException("Failed to search shops", e);
        }
    }
}
