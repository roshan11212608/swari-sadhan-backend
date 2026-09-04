package swari.sewa.module.vehicle.service;

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
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.common.enums.VehicleType;
import swari.sewa.module.category.entity.Category;
import swari.sewa.module.category.repository.CategoryRepository;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.vehicle.dto.VehicleDto;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.module.vehicle.repository.SellVehicleApplicationRepository;
import swari.sewa.module.vehicle.service.impl.VehicleServiceImpl;
import swari.sewa.module.vehicle.service.SellApplicationService;
import swari.sewa.module.vehicle.service.SellVehicleApplicationService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VehicleServiceImpl query optimization changes.
 *
 * <p>Verifies that list endpoints use the JOIN FETCH repository methods
 * (WithDetails variants) instead of the lazy-loading variants, and that
 * the list mapper does NOT trigger sell-application lookups.
 */
@ExtendWith(MockitoExtension.class)
class VehicleServiceQueryOptimizationTest {

    @Mock private VehicleRepository vehicleRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private SellApplicationService sellApplicationService;
    @Mock private SellVehicleApplicationService sellVehicleApplicationService;
    @Mock private SellVehicleApplicationRepository sellVehicleApplicationRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private Vehicle createTestVehicle(Long id, VehicleStatus status) {
        Vehicle v = new Vehicle();
        v.setId(id);
        v.setStatus(status);
        v.setTitle("Test Vehicle " + id);
        v.setViewCount(0L);
        v.setContactCount(0L);

        // Set up shop with shopOwner so mapToDtoCommon doesn't NPE
        ShopOwner owner = new ShopOwner();
        owner.setId(id);
        owner.setProvince("Bagmati");
        owner.setDistrict("Kathmandu");
        owner.setMunicipality("KMC");
        owner.setWard("1");
        owner.setTole("Test");

        Shop shop = new Shop();
        shop.setId(id);
        shop.setName("Test Shop " + id);
        shop.setCity("Kathmandu");
        shop.setPhoneNumber("9801234567");
        shop.setEmailAddress("shop@test.com");
        shop.setAddressLine1("Test Address");
        shop.setShopOwner(owner);

        Category category = new Category();
        category.setId(id);
        category.setName("Bike");

        v.setShop(shop);
        v.setCategory(category);
        return v;
    }

    @BeforeEach
    void setUpMapper() {
        // ModelMapper returns a basic DTO; the service then sets fields manually
        when(modelMapper.map(any(Vehicle.class), eq(VehicleDto.class)))
                .thenAnswer(inv -> {
                    Vehicle v = inv.getArgument(0);
                    VehicleDto dto = new VehicleDto();
                    dto.setId(v.getId());
                    dto.setTitle(v.getTitle());
                    dto.setStatus(v.getStatus());
                    return dto;
                });
    }

    @Test
    void getActiveVehicles_usesJoinFetchVariant() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vehicle> page = new PageImpl<>(List.of(createTestVehicle(1L, VehicleStatus.ACTIVE)));
        when(vehicleRepository.findActiveVehiclesWithDetails(pageable)).thenReturn(page);

        Page<VehicleDto> result = vehicleService.getActiveVehicles(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(vehicleRepository).findActiveVehiclesWithDetails(pageable);
        verify(vehicleRepository, never()).findActiveVehicles(any());
    }

    @Test
    void getInactiveVehicles_usesJoinFetchVariant() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vehicle> page = new PageImpl<>(List.of(createTestVehicle(1L, VehicleStatus.INACTIVE)));
        when(vehicleRepository.findInactiveVehiclesWithDetails(pageable)).thenReturn(page);

        Page<VehicleDto> result = vehicleService.getInactiveVehicles(0, 10);

        assertNotNull(result);
        verify(vehicleRepository).findInactiveVehiclesWithDetails(pageable);
        verify(vehicleRepository, never()).findInactiveVehicles(any());
    }

    @Test
    void getFeaturedVehicles_usesJoinFetchVariant() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vehicle> page = new PageImpl<>(List.of(createTestVehicle(1L, VehicleStatus.ACTIVE)));
        when(vehicleRepository.findFeaturedVehiclesWithDetails(pageable)).thenReturn(page);

        Page<VehicleDto> result = vehicleService.getFeaturedVehicles(0, 10);

        assertNotNull(result);
        verify(vehicleRepository).findFeaturedVehiclesWithDetails(pageable);
        verify(vehicleRepository, never()).findFeaturedVehicles(any());
    }

    @Test
    void getVehiclesByShop_usesJoinFetchVariant() {
        Long shopId = 5L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vehicle> page = new PageImpl<>(List.of(createTestVehicle(1L, VehicleStatus.ACTIVE)));
        when(vehicleRepository.findByShopIdWithDetails(shopId, pageable)).thenReturn(page);

        Page<VehicleDto> result = vehicleService.getVehiclesByShop(shopId, 0, 10);

        assertNotNull(result);
        verify(vehicleRepository).findByShopIdWithDetails(shopId, pageable);
        verify(vehicleRepository, never()).findByShopId(any(), any());
    }

    @Test
    void getVehiclesByCategory_usesJoinFetchVariant() {
        Long categoryId = 3L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vehicle> page = new PageImpl<>(List.of(createTestVehicle(1L, VehicleStatus.ACTIVE)));
        when(vehicleRepository.findByCategoryIdWithDetails(categoryId, pageable)).thenReturn(page);

        Page<VehicleDto> result = vehicleService.getVehiclesByCategory(categoryId, 0, 10);

        assertNotNull(result);
        verify(vehicleRepository).findByCategoryIdWithDetails(categoryId, pageable);
        verify(vehicleRepository, never()).findByCategoryId(any(), any());
    }

    @Test
    void getVehiclesByType_usesJoinFetchVariant() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vehicle> page = new PageImpl<>(List.of(createTestVehicle(1L, VehicleStatus.ACTIVE)));
        when(vehicleRepository.findByVehicleTypeWithDetails(VehicleType.BIKE, pageable)).thenReturn(page);

        Page<VehicleDto> result = vehicleService.getVehiclesByType(VehicleType.BIKE, 0, 10);

        assertNotNull(result);
        verify(vehicleRepository).findByVehicleTypeWithDetails(VehicleType.BIKE, pageable);
        verify(vehicleRepository, never()).findByVehicleType(any(), any());
    }

    @Test
    void getAllVehicles_usesJoinFetchVariant() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vehicle> page = new PageImpl<>(List.of(createTestVehicle(1L, VehicleStatus.ACTIVE)));
        when(vehicleRepository.findByStatusNotWithDetails(VehicleStatus.SOLD, pageable)).thenReturn(page);

        Page<VehicleDto> result = vehicleService.getAllVehicles(0, 10);

        assertNotNull(result);
        verify(vehicleRepository).findByStatusNotWithDetails(VehicleStatus.SOLD, pageable);
    }

    @Test
    void listMapper_doesNotQuerySellApplicationsForSoldVehicles() {
        // A list of vehicles that includes a SOLD vehicle should NOT trigger
        // sellVehicleApplicationRepository.findByVehicleId for each SOLD vehicle.
        Pageable pageable = PageRequest.of(0, 10);
        Vehicle soldVehicle = createTestVehicle(1L, VehicleStatus.SOLD);
        Vehicle activeVehicle = createTestVehicle(2L, VehicleStatus.ACTIVE);
        Page<Vehicle> page = new PageImpl<>(List.of(soldVehicle, activeVehicle));
        when(vehicleRepository.findByStatusNotWithDetails(VehicleStatus.SOLD, pageable)).thenReturn(page);

        vehicleService.getAllVehicles(0, 10);

        // The list mapper should NOT call sellVehicleApplicationRepository
        verify(sellVehicleApplicationRepository, never()).findByVehicleId(any());
    }

    @Test
    void detailMapper_queriesSellApplicationsForSoldVehicles() {
        // The detail endpoint (getVehicleById) should still query sell applications
        // for SOLD vehicles to show the offered price.
        Vehicle soldVehicle = createTestVehicle(1L, VehicleStatus.SOLD);
        when(vehicleRepository.findById(1L)).thenReturn(java.util.Optional.of(soldVehicle));
        when(sellVehicleApplicationRepository.findByVehicleId(1L)).thenReturn(List.of());

        vehicleService.getVehicleById(1L);

        verify(sellVehicleApplicationRepository).findByVehicleId(1L);
    }
}
