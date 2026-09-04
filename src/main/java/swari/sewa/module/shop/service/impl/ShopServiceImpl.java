package swari.sewa.module.shop.service.impl;

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
import swari.sewa.module.shop.repository.ShopReviewRepository;
import swari.sewa.module.shop.service.ShopService;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.common.enums.ShopStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final ShopReviewRepository shopReviewRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final VehicleRepository vehicleRepository;
    private final ModelMapper modelMapper;

    private ShopDto mapToDto(Shop shop, java.util.Map<Long, Long> vehicleCountMap,
                             java.util.Map<Long, long[]> reviewStatsMap) {
        // reviewStatsMap: shopId -> [count, avgRating (as long bits)]
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
        dto.setDisplayOrder(shop.getDisplayOrder());
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

        // Use pre-fetched review stats map instead of per-shop queries
        long[] reviewStats = reviewStatsMap.getOrDefault(shop.getId(), new long[]{0, 0});
        long reviewCount = reviewStats[0];
        double avgRating = Double.longBitsToDouble(reviewStats[1]);
        dto.setRating(Math.round(avgRating * 10.0) / 10.0);
        dto.setReviewCount((int) reviewCount);
        dto.setTotalReviews((int) reviewCount);

        return dto;
    }

    // Overloaded method for backward compatibility (used in single-shop operations)
    private ShopDto mapToDto(Shop shop) {
        java.util.Map<Long, Long> vehicleCountMap = java.util.Collections.singletonMap(
            shop.getId(),
            vehicleRepository.countByShopId(shop.getId())
        );
        java.util.Map<Long, long[]> reviewStatsMap = buildReviewStatsMap(
                java.util.Collections.singletonList(shop.getId()));
        return mapToDto(shop, vehicleCountMap, reviewStatsMap);
    }

    /**
     * Builds a Map of shopId -> [count, avgRatingBits] using a single batch query.
     * Eliminates N+1 review count/avg queries when mapping a list of shops.
     */
    private java.util.Map<Long, long[]> buildReviewStatsMap(java.util.List<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return new java.util.HashMap<>();
        }
        List<Object[]> rows = shopReviewRepository.countAndAvgRatingByShopIds(shopIds);
        java.util.Map<Long, long[]> map = new java.util.HashMap<>();
        for (Object[] row : rows) {
            Long shopId = (Long) row[0];
            Long count = (Long) row[1];
            Double avg = (Double) row[2];
            map.put(shopId, new long[]{count, Double.doubleToRawLongBits(avg != null ? avg : 0.0)});
        }
        // Ensure shops with no reviews have a default entry
        for (Long shopId : shopIds) {
            map.putIfAbsent(shopId, new long[]{0, 0});
        }
        return map;
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

        // display_order is NOT NULL in DB — ensure it's never null
        if (shop.getDisplayOrder() == null) {
            shop.setDisplayOrder(0);
        }

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
    public Optional<ShopDto> getShopByEmail(String email) {
        // Try user email first, then shop owner email
        Optional<Shop> shop = shopRepository.findByUserEmail(email);
        if (shop.isEmpty()) {
            shop = shopRepository.findByShopOwnerEmail(email);
        }
        return shop.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> getAllShops() {
        try {
            // Fetch only ACTIVE shops for the public listing — suspended/inactive/pending shops are excluded
            List<Shop> shops = shopRepository.findAllActiveWithShopOwner();

            // Fetch all vehicle counts in one aggregation query
            java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMap();

            // Fetch review stats in one batch query
            java.util.List<Long> shopIds = shops.stream().map(Shop::getId).collect(Collectors.toList());
            java.util.Map<Long, long[]> reviewStatsMap = buildReviewStatsMap(shopIds);

            // Map to DTOs using pre-fetched counts
            return shops.stream()
                    .map(shop -> mapToDto(shop, vehicleCountMap, reviewStatsMap))
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
            // Fetch only ACTIVE shops for the public listing — suspended/inactive/pending shops are excluded
            Page<Shop> shopsPage = shopRepository.findAllActiveWithShopOwner(pageable);

            // Fetch vehicle counts only for shops in current page
            java.util.List<Long> shopIds = shopsPage.getContent().stream()
                    .map(Shop::getId)
                    .collect(Collectors.toList());
            java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
            java.util.Map<Long, long[]> reviewStatsMap = buildReviewStatsMap(shopIds);

            // Map to DTOs using pre-fetched counts
            java.util.List<ShopDto> dtoList = shopsPage.getContent().stream()
                    .map(shop -> mapToDto(shop, vehicleCountMap, reviewStatsMap))
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
     * Filters in SQL instead of fetching all shops' counts and filtering in Java.
     */
    private java.util.Map<Long, Long> buildVehicleCountMapForShopIds(java.util.List<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return new java.util.HashMap<>();
        }

        java.util.List<java.util.Map<String, Object>> results = vehicleRepository.countVehiclesByShopIds(shopIds);
        java.util.Map<Long, Long> countMap = new java.util.HashMap<>();

        for (java.util.Map<String, Object> result : results) {
            Long shopId = (Long) result.get("shopId");
            Long count = (Long) result.get("count");
            if (shopId != null && count != null) {
                countMap.put(shopId, count);
            }
        }

        for (Long shopId : shopIds) {
            countMap.putIfAbsent(shopId, 0L);
        }

        return countMap;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> getShopsByCity(String city) {
        List<Shop> shops = shopRepository.findByCityAndStatusActiveWithShopOwner(city);
        java.util.List<Long> shopIds = shops.stream().map(Shop::getId).collect(Collectors.toList());
        java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
        java.util.Map<Long, long[]> reviewStatsMap = buildReviewStatsMap(shopIds);
        return shops.stream().map(shop -> mapToDto(shop, vehicleCountMap, reviewStatsMap)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopDto> getShopsByCity(String city, Pageable pageable) {
        try {
            Page<Shop> shopsPage = shopRepository.findByCityAndStatusActiveWithShopOwner(city, pageable);
            java.util.List<Long> shopIds = shopsPage.getContent().stream().map(Shop::getId).collect(Collectors.toList());
            java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
            java.util.Map<Long, long[]> reviewStatsMap = buildReviewStatsMap(shopIds);

            java.util.List<ShopDto> dtoList = shopsPage.getContent().stream()
                    .map(shop -> mapToDto(shop, vehicleCountMap, reviewStatsMap))
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
        List<Shop> shops = shopRepository.findByStateAndStatusActiveWithShopOwner(state);
        java.util.List<Long> shopIds = shops.stream().map(Shop::getId).collect(Collectors.toList());
        java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
        java.util.Map<Long, long[]> reviewStatsMap = buildReviewStatsMap(shopIds);
        return shops.stream().map(shop -> mapToDto(shop, vehicleCountMap, reviewStatsMap)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopDto> getShopsByState(String state, Pageable pageable) {
        try {
            Page<Shop> shopsPage = shopRepository.findByStateAndStatusActiveWithShopOwner(state, pageable);
            java.util.List<Long> shopIds = shopsPage.getContent().stream().map(Shop::getId).collect(Collectors.toList());
            java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
            java.util.Map<Long, long[]> reviewStatsMap = buildReviewStatsMap(shopIds);

            java.util.List<ShopDto> dtoList = shopsPage.getContent().stream()
                    .map(shop -> mapToDto(shop, vehicleCountMap, reviewStatsMap))
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
        Page<Shop> shopsPage = shopRepository.findFeaturedShopsWithShopOwner(Pageable.ofSize(10));
        java.util.List<Long> shopIds = shopsPage.getContent().stream().map(Shop::getId).collect(Collectors.toList());
        java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
        java.util.Map<Long, long[]> reviewStatsMap = buildReviewStatsMap(shopIds);
        return shopsPage.getContent().stream()
                .map(shop -> mapToDto(shop, vehicleCountMap, reviewStatsMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopDto> getFeaturedShops(Pageable pageable) {
        try {
            Page<Shop> shopsPage = shopRepository.findFeaturedShopsWithShopOwner(pageable);
            java.util.List<Long> shopIds = shopsPage.getContent().stream().map(Shop::getId).collect(Collectors.toList());
            java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
            java.util.Map<Long, long[]> reviewStatsMap = buildReviewStatsMap(shopIds);

            java.util.List<ShopDto> dtoList = shopsPage.getContent().stream()
                    .map(shop -> mapToDto(shop, vehicleCountMap, reviewStatsMap))
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
        // display_order is NOT NULL in DB — preserve existing value if DTO sends null
        if (shop.getDisplayOrder() == null) {
            shop.setDisplayOrder(0);
        }
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
    @Transactional
    public void reorderShops(List<swari.sewa.module.shop.dto.ShopReorderDto> reorders) {
        if (reorders == null || reorders.isEmpty()) {
            throw new IllegalArgumentException("Reorder list cannot be empty");
        }
        // Validate no duplicate shop IDs
        java.util.Set<Long> seenIds = new java.util.HashSet<>();
        // Validate no duplicate displayOrders
        java.util.Set<Integer> seenOrders = new java.util.HashSet<>();
        for (swari.sewa.module.shop.dto.ShopReorderDto dto : reorders) {
            if (dto.getId() == null) {
                throw new IllegalArgumentException("Shop id cannot be null");
            }
            if (dto.getDisplayOrder() == null || dto.getDisplayOrder() < 0) {
                throw new IllegalArgumentException("Display order must be >= 0 for shop id: " + dto.getId());
            }
            if (!seenIds.add(dto.getId())) {
                throw new IllegalArgumentException("Duplicate shop id in reorder request: " + dto.getId());
            }
            if (!seenOrders.add(dto.getDisplayOrder())) {
                throw new IllegalArgumentException("Duplicate display order in reorder request: " + dto.getDisplayOrder());
            }
        }
        for (swari.sewa.module.shop.dto.ShopReorderDto dto : reorders) {
            Shop shop = shopRepository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + dto.getId()));
            shop.setDisplayOrder(dto.getDisplayOrder());
            shopRepository.save(shop);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByLicenseNumber(String licenseNumber) {
        return shopRepository.existsByLicenseNumber(licenseNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> searchShops(String keyword) {
        List<Shop> shops = shopRepository.searchByKeywordWithShopOwner(keyword);
        java.util.List<Long> shopIds = shops.stream().map(Shop::getId).collect(Collectors.toList());
        java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
        java.util.Map<Long, long[]> reviewStatsMap = buildReviewStatsMap(shopIds);
        return shops.stream().map(shop -> mapToDto(shop, vehicleCountMap, reviewStatsMap)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopDto> searchShops(String keyword, Pageable pageable) {
        try {
            Page<Shop> shopsPage = shopRepository.searchByKeywordWithShopOwner(keyword, pageable);
            java.util.List<Long> shopIds = shopsPage.getContent().stream().map(Shop::getId).collect(Collectors.toList());
            java.util.Map<Long, Long> vehicleCountMap = buildVehicleCountMapForShopIds(shopIds);
            java.util.Map<Long, long[]> reviewStatsMap = buildReviewStatsMap(shopIds);

            java.util.List<ShopDto> dtoList = shopsPage.getContent().stream()
                    .map(shop -> mapToDto(shop, vehicleCountMap, reviewStatsMap))
                    .collect(Collectors.toList());

            return new org.springframework.data.domain.PageImpl<>(dtoList, pageable, shopsPage.getTotalElements());
        } catch (Exception e) {
            log.error("Error searching paginated shops: {}", e.getMessage(), e);
            throw new ShopServiceException("Failed to search shops", e);
        }
    }
}
