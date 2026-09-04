package swari.sewa.module.wishlist.service;

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
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.common.exception.WishlistAlreadyExistsException;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.module.wishlist.dto.WishlistDto;
import swari.sewa.module.wishlist.entity.Wishlist;
import swari.sewa.module.wishlist.repository.WishlistRepository;
import swari.sewa.module.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WishlistServiceImpl query optimization changes.
 *
 * <p>Verifies that list endpoints use JOIN FETCH repository variants
 * (findByCustomer_IdWithCustomerVehicleShop, findByVehicle_Shop_IdWithCustomerVehicleShop)
 * instead of the plain findByCustomer_Id/findByVehicle_Shop_Id methods that trigger
 * N+1 lazy loads on customer, vehicle, and vehicle.shop relationships.
 *
 * <p>Also verifies that addToWishlist uses existsById + getReferenceById
 * instead of loading full User and Vehicle entities via findById.
 */
@ExtendWith(MockitoExtension.class)
class WishlistServiceQueryOptimizationTest {

    @Mock private WishlistRepository wishlistRepository;
    @Mock private UserRepository userRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private User createTestUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Customer");
        user.setLastName(String.valueOf(id));
        user.setEmail("customer" + id + "@test.com");
        user.setPhone("1234567890");
        return user;
    }

    private Vehicle createTestVehicle(Long id) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setName("Test Shop " + id);

        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setTitle("Test Vehicle " + id);
        vehicle.setShop(shop);
        return vehicle;
    }

    private Wishlist createTestWishlist(Long id) {
        User customer = createTestUser(id);
        Vehicle vehicle = createTestVehicle(id);

        Wishlist wishlist = new Wishlist();
        wishlist.setId(id);
        wishlist.setCustomer(customer);
        wishlist.setVehicle(vehicle);
        return wishlist;
    }

    private void setupMapper(Wishlist wishlist) {
        WishlistDto dto = new WishlistDto();
        dto.setId(wishlist.getId());
        lenient().when(modelMapper.map(eq(wishlist), eq(WishlistDto.class))).thenReturn(dto);
    }

    @Test
    void getCustomerWishlist_paginated_usesJoinFetchVariant() {
        Wishlist wishlist = createTestWishlist(1L);
        setupMapper(wishlist);
        Page<Wishlist> page = new PageImpl<>(List.of(wishlist));
        when(wishlistRepository.findByCustomer_IdWithCustomerVehicleShop(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<WishlistDto> result = wishlistService.getCustomerWishlist(1L, 0, 10);

        assertNotNull(result);
        verify(wishlistRepository).findByCustomer_IdWithCustomerVehicleShop(eq(1L), any(Pageable.class));
        verify(wishlistRepository, never()).findByCustomer_Id(anyLong(), any(Pageable.class));
    }

    @Test
    void getCustomerWishlist_all_usesJoinFetchVariant() {
        Wishlist wishlist = createTestWishlist(1L);
        setupMapper(wishlist);
        when(wishlistRepository.findByCustomer_IdWithCustomerVehicleShop(eq(1L)))
                .thenReturn(List.of(wishlist));

        List<WishlistDto> result = wishlistService.getCustomerWishlist(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(wishlistRepository).findByCustomer_IdWithCustomerVehicleShop(eq(1L));
        verify(wishlistRepository, never()).findByCustomer_Id(anyLong());
    }

    @Test
    void getShopWishlist_usesJoinFetchVariant() {
        Wishlist wishlist = createTestWishlist(1L);
        setupMapper(wishlist);
        when(wishlistRepository.findByVehicle_Shop_IdWithCustomerVehicleShop(eq(1L)))
                .thenReturn(List.of(wishlist));

        List<WishlistDto> result = wishlistService.getShopWishlist(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(wishlistRepository).findByVehicle_Shop_IdWithCustomerVehicleShop(eq(1L));
        verify(wishlistRepository, never()).findByVehicle_Shop_Id(anyLong());
    }

    @Test
    void getWishlistById_usesJoinFetchVariant_notFindById() {
        Wishlist wishlist = createTestWishlist(1L);
        setupMapper(wishlist);
        when(wishlistRepository.findByIdWithCustomerVehicleShop(eq(1L)))
                .thenReturn(Optional.of(wishlist));

        Optional<WishlistDto> result = wishlistService.getWishlistById(1L);

        assertTrue(result.isPresent());
        verify(wishlistRepository).findByIdWithCustomerVehicleShop(eq(1L));
        verify(wishlistRepository, never()).findById(anyLong());
    }

    @Test
    void addToWishlist_usesExistsByIdAndReference_notFindById() {
        when(wishlistRepository.existsByCustomerIdAndVehicleId(1L, 2L)).thenReturn(false);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(vehicleRepository.existsById(2L)).thenReturn(true);
        when(userRepository.getReferenceById(1L)).thenReturn(createTestUser(1L));
        when(vehicleRepository.getReferenceById(2L)).thenReturn(createTestVehicle(2L));

        Wishlist savedWishlist = createTestWishlist(1L);
        savedWishlist.setVehicle(createTestVehicle(2L));
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(savedWishlist);
        when(wishlistRepository.findByIdWithCustomerVehicleShop(any())).thenReturn(Optional.of(savedWishlist));
        setupMapper(savedWishlist);

        WishlistDto result = wishlistService.addToWishlist(1L, 2L);

        assertNotNull(result);
        // Should use existsById, not findById
        verify(userRepository).existsById(1L);
        verify(vehicleRepository).existsById(2L);
        verify(userRepository).getReferenceById(1L);
        verify(vehicleRepository).getReferenceById(2L);
        // Should NOT call findById for user or vehicle
        verify(userRepository, never()).findById(anyLong());
        verify(vehicleRepository, never()).findById(anyLong());
    }

    @Test
    void addToWishlist_throwsWhenCustomerNotFound() {
        when(wishlistRepository.existsByCustomerIdAndVehicleId(1L, 2L)).thenReturn(false);
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.addToWishlist(1L, 2L));
    }

    @Test
    void addToWishlist_throwsWhenVehicleNotFound() {
        when(wishlistRepository.existsByCustomerIdAndVehicleId(1L, 2L)).thenReturn(false);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(vehicleRepository.existsById(2L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.addToWishlist(1L, 2L));
    }

    @Test
    void addToWishlist_throwsWhenAlreadyExists() {
        when(wishlistRepository.existsByCustomerIdAndVehicleId(1L, 2L)).thenReturn(true);

        assertThrows(WishlistAlreadyExistsException.class, () -> wishlistService.addToWishlist(1L, 2L));
    }

    @Test
    void updateRemark_usesJoinFetchVariantForResponse() {
        Wishlist wishlist = createTestWishlist(1L);
        when(wishlistRepository.findById(1L)).thenReturn(Optional.of(wishlist));
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(wishlist);
        when(wishlistRepository.findByIdWithCustomerVehicleShop(1L)).thenReturn(Optional.of(wishlist));
        setupMapper(wishlist);

        WishlistDto result = wishlistService.updateRemark(1L, "test remark");

        assertNotNull(result);
        // After save, should reload with JOIN FETCH
        verify(wishlistRepository).findByIdWithCustomerVehicleShop(1L);
    }
}
