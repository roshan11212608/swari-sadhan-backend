package swari.sewa.module.shop.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import swari.sewa.common.enums.ShopStatus;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.shop.service.impl.AdminShopServiceImpl;
import swari.sewa.module.user.entity.ShopOwner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminShopServiceImpl query optimization.
 *
 * <p>Verifies that admin shop list endpoints use JOIN FETCH variants
 * (findAllWithShopOwner, findByStatusWithShopOwner, searchByKeywordWithShopOwnerPaged)
 * instead of lazy-loading shopOwner per shop.
 */
@ExtendWith(MockitoExtension.class)
class AdminShopServiceQueryOptimizationTest {

    @Mock private ShopRepository shopRepository;

    @InjectMocks
    private AdminShopServiceImpl adminShopService;

    private Shop createTestShop(Long id) {
        ShopOwner owner = new ShopOwner();
        owner.setId(id);
        owner.setFirstName("Owner");
        owner.setLastName(String.valueOf(id));
        owner.setEmail("owner" + id + "@test.com");

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

    @Test
    void getAllShops_noFilters_usesJoinFetchVariant() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Shop> page = new PageImpl<>(List.of(createTestShop(1L)), pageable, 1);
        when(shopRepository.findAllWithShopOwner(pageable)).thenReturn(page);

        Page<Object> result = adminShopService.getAllShops(pageable, null, null);

        assertNotNull(result);
        verify(shopRepository).findAllWithShopOwner(pageable);
        verify(shopRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllShops_withStatus_usesJoinFetchVariant() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Shop> page = new PageImpl<>(List.of(createTestShop(1L)), pageable, 1);
        when(shopRepository.findByStatusWithShopOwner(ShopStatus.ACTIVE, pageable)).thenReturn(page);

        Page<Object> result = adminShopService.getAllShops(pageable, null, "ACTIVE");

        assertNotNull(result);
        verify(shopRepository).findByStatusWithShopOwner(ShopStatus.ACTIVE, pageable);
        verify(shopRepository, never()).findByStatus(any(ShopStatus.class), any(Pageable.class));
    }

    @Test
    void getAllShops_withSearch_usesJoinFetchVariant() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Shop> page = new PageImpl<>(List.of(createTestShop(1L)), pageable, 1);
        when(shopRepository.searchByKeywordWithShopOwnerPaged("test", pageable)).thenReturn(page);

        Page<Object> result = adminShopService.getAllShops(pageable, "test", null);

        assertNotNull(result);
        verify(shopRepository).searchByKeywordWithShopOwnerPaged("test", pageable);
    }

    @Test
    void getPendingShops_usesJoinFetchVariant() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Shop> page = new PageImpl<>(List.of(createTestShop(1L)), pageable, 1);
        when(shopRepository.findByStatusWithShopOwner(ShopStatus.PENDING_APPROVAL, pageable)).thenReturn(page);

        Page<Object> result = adminShopService.getPendingShops(pageable);

        assertNotNull(result);
        verify(shopRepository).findByStatusWithShopOwner(ShopStatus.PENDING_APPROVAL, pageable);
        verify(shopRepository, never()).findByStatus(any(ShopStatus.class), any(Pageable.class));
    }

    @Test
    void getShopById_usesJoinFetchVariant() {
        Shop shop = createTestShop(1L);
        when(shopRepository.findByIdWithShopOwner(1L)).thenReturn(java.util.Optional.of(shop));

        Object result = adminShopService.getShopById(1L);

        assertNotNull(result);
        verify(shopRepository).findByIdWithShopOwner(1L);
        verify(shopRepository, never()).findById(any());
    }
}
