package swari.sewa.module.shop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import swari.sewa.common.enums.ShopStatus;
import swari.sewa.module.shop.dto.ShopDto;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.shop.repository.ShopReviewRepository;
import swari.sewa.module.shop.service.impl.ShopServiceImpl;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ShopServiceImpl query optimization changes.
 *
 * <p>Verifies that list endpoints use batched review stats queries
 * instead of per-shop countByShopId + getAverageRatingByShopId calls,
 * and that buildVehicleCountMapForShopIds uses SQL-filtered queries.
 */
@ExtendWith(MockitoExtension.class)
class ShopServiceQueryOptimizationTest {

    @Mock private ShopRepository shopRepository;
    @Mock private ShopReviewRepository shopReviewRepository;
    @Mock private ShopOwnerRepository shopOwnerRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private ShopServiceImpl shopService;

    private Shop createTestShop(Long id) {
        ShopOwner owner = new ShopOwner();
        owner.setId(id);
        owner.setFirstName("Owner");
        owner.setLastName(String.valueOf(id));
        owner.setEmail("owner" + id + "@test.com");
        owner.setKycVerified(true);

        Shop shop = new Shop();
        shop.setId(id);
        shop.setName("Test Shop " + id);
        shop.setCity("Kathmandu");
        shop.setState("Bagmati");
        shop.setCountry("Nepal");
        shop.setLicenseNumber("LIC-" + id);
        shop.setPhoneNumber("9801234567");
        shop.setEmailAddress("shop" + id + "@test.com");
        shop.setStatus(ShopStatus.ACTIVE);
        shop.setShopOwner(owner);
        return shop;
    }

    @BeforeEach
    void setUpMocks() {
        // Mock batch vehicle count query (SQL-filtered) — lenient since not all tests use it
        lenient().when(vehicleRepository.countVehiclesByShopIds(any()))
                .thenAnswer(inv -> {
                    List<Long> shopIds = inv.getArgument(0);
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (Long sid : shopIds) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("shopId", sid);
                        row.put("count", 5L);
                        results.add(row);
                    }
                    return results;
                });

        // Mock batch review stats query — lenient since not all tests use it
        lenient().when(shopReviewRepository.countAndAvgRatingByShopIds(any()))
                .thenAnswer(inv -> {
                    List<Long> shopIds = inv.getArgument(0);
                    List<Object[]> results = new ArrayList<>();
                    for (Long sid : shopIds) {
                        results.add(new Object[]{sid, 3L, 4.5});
                    }
                    return results;
                });
    }

    @Test
    void getAllShops_usesBatchedReviewStats_notPerShopQueries() {
        List<Shop> shops = List.of(createTestShop(1L), createTestShop(2L), createTestShop(3L));
        when(shopRepository.findAllWithShopOwner()).thenReturn(shops);
        lenient().when(vehicleRepository.countVehiclesByShopGrouped()).thenReturn(Collections.emptyList());

        List<ShopDto> result = shopService.getAllShops();

        assertNotNull(result);
        assertEquals(3, result.size());
        // CRITICAL: verify batch review stats query was called once, not 3×2=6 times
        verify(shopReviewRepository, times(1)).countAndAvgRatingByShopIds(any());
        // Verify per-shop review queries were NOT called
        verify(shopReviewRepository, never()).countByShopId(any());
        verify(shopReviewRepository, never()).getAverageRatingByShopId(any());
    }

    @Test
    void getAllShops_paginated_usesBatchedReviewStats() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Shop> shops = List.of(createTestShop(1L), createTestShop(2L));
        Page<Shop> page = new PageImpl<>(shops, pageable, 2);
        when(shopRepository.findAllWithShopOwner(pageable)).thenReturn(page);

        Page<ShopDto> result = shopService.getAllShops(pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(shopReviewRepository, times(1)).countAndAvgRatingByShopIds(any());
        verify(shopReviewRepository, never()).countByShopId(any());
    }

    @Test
    void getFeaturedShops_usesBatchedReviewStats() {
        Pageable pageable = Pageable.ofSize(10);
        List<Shop> shops = List.of(createTestShop(1L), createTestShop(2L));
        Page<Shop> page = new PageImpl<>(shops, pageable, 2);
        when(shopRepository.findFeaturedShopsWithShopOwner(pageable)).thenReturn(page);

        List<ShopDto> result = shopService.getFeaturedShops();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(shopReviewRepository, times(1)).countAndAvgRatingByShopIds(any());
        verify(shopReviewRepository, never()).countByShopId(any());
    }

    @Test
    void getShopsByCity_usesBatchedVehicleCounts_notPerShopQueries() {
        List<Shop> shops = List.of(createTestShop(1L), createTestShop(2L));
        when(shopRepository.findByCityAndStatusActiveWithShopOwner("Kathmandu")).thenReturn(shops);

        List<ShopDto> result = shopService.getShopsByCity("Kathmandu");

        assertNotNull(result);
        assertEquals(2, result.size());
        // Non-paginated variant should use batched vehicle counts, not per-shop countByShopId
        verify(vehicleRepository, times(1)).countVehiclesByShopIds(any());
        verify(vehicleRepository, never()).countByShopId(any());
    }

    @Test
    void getShopsByState_usesBatchedVehicleCounts_notPerShopQueries() {
        List<Shop> shops = List.of(createTestShop(1L), createTestShop(2L));
        when(shopRepository.findByStateAndStatusActiveWithShopOwner("Bagmati")).thenReturn(shops);

        List<ShopDto> result = shopService.getShopsByState("Bagmati");

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(vehicleRepository, times(1)).countVehiclesByShopIds(any());
        verify(vehicleRepository, never()).countByShopId(any());
    }

    @Test
    void searchShops_usesBatchedVehicleCounts_notPerShopQueries() {
        List<Shop> shops = List.of(createTestShop(1L), createTestShop(2L));
        when(shopRepository.searchByKeywordWithShopOwner("test")).thenReturn(shops);

        List<ShopDto> result = shopService.searchShops("test");

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(vehicleRepository, times(1)).countVehiclesByShopIds(any());
        verify(vehicleRepository, never()).countByShopId(any());
    }

    @Test
    void buildVehicleCountMapForShopIds_usesSqlFilteredQuery_notAllShopsQuery() {
        List<Long> shopIds = List.of(1L, 2L, 3L);
        when(vehicleRepository.countVehiclesByShopIds(shopIds)).thenReturn(Collections.emptyList());

        // Use the paginated endpoint to trigger buildVehicleCountMapForShopIds
        Pageable pageable = PageRequest.of(0, 10);
        List<Shop> shops = List.of(createTestShop(1L), createTestShop(2L), createTestShop(3L));
        Page<Shop> page = new PageImpl<>(shops, pageable, 3);
        when(shopRepository.findAllWithShopOwner(pageable)).thenReturn(page);

        shopService.getAllShops(pageable);

        // CRITICAL: verify SQL-filtered query was used, not the all-shops grouped query
        verify(vehicleRepository, times(1)).countVehiclesByShopIds(shopIds);
        verify(vehicleRepository, never()).countVehiclesByShopGrouped();
    }

    @Test
    void mapToDto_includesCorrectReviewCountAndRating() {
        List<Shop> shops = List.of(createTestShop(1L));
        when(shopRepository.findAllWithShopOwner()).thenReturn(shops);
        lenient().when(vehicleRepository.countVehiclesByShopGrouped()).thenReturn(Collections.emptyList());

        List<ShopDto> result = shopService.getAllShops();

        assertNotNull(result);
        assertEquals(1, result.size());
        ShopDto dto = result.get(0);
        // From the mock: count=3, avgRating=4.5
        assertEquals(3, dto.getReviewCount());
        assertEquals(4.5, dto.getRating());
    }
}
