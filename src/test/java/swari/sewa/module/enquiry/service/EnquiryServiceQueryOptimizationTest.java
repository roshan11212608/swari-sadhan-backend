package swari.sewa.module.enquiry.service;

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
import swari.sewa.common.enums.EnquiryStatus;
import swari.sewa.module.enquiry.dto.EnquiryDto;
import swari.sewa.module.enquiry.entity.Enquiry;
import swari.sewa.module.enquiry.repository.EnquiryMessageRepository;
import swari.sewa.module.enquiry.repository.EnquiryRepository;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnquiryServiceImpl query optimization changes.
 *
 * <p>Verifies that list endpoints use JOIN FETCH repository variants
 * (findAllWithCustomerVehicleShop, findByShopIdWithCustomerVehicleShop, etc.)
 * instead of the plain findAll/findByShopId methods that trigger N+1 lazy loads
 * on customer, vehicle, and shop relationships.
 */
@ExtendWith(MockitoExtension.class)
class EnquiryServiceQueryOptimizationTest {

    @Mock private EnquiryRepository enquiryRepository;
    @Mock private EnquiryMessageRepository enquiryMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private EnquiryServiceImpl enquiryService;

    private Enquiry createTestEnquiry(Long id) {
        User customer = new User();
        customer.setId(id);
        customer.setFirstName("Customer");
        customer.setLastName(String.valueOf(id));
        customer.setEmail("customer" + id + "@test.com");
        customer.setPhone("1234567890");

        ShopOwner shopOwner = new ShopOwner();
        shopOwner.setId(id);
        shopOwner.setEmail("owner" + id + "@test.com");

        Shop shop = new Shop();
        shop.setId(id);
        shop.setName("Test Shop " + id);
        shop.setShopOwner(shopOwner);

        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setTitle("Test Vehicle " + id);
        vehicle.setShop(shop);

        Enquiry enquiry = new Enquiry();
        enquiry.setId(id);
        enquiry.setCustomerName("Customer " + id);
        enquiry.setCustomerEmail("customer" + id + "@test.com");
        enquiry.setCustomerPhone("1234567890");
        enquiry.setMessage("Test message " + id);
        enquiry.setStatus(EnquiryStatus.PENDING);
        enquiry.setCustomer(customer);
        enquiry.setVehicle(vehicle);
        enquiry.setShop(shop);

        return enquiry;
    }

    private void setupMapper(Enquiry enquiry) {
        EnquiryDto dto = new EnquiryDto();
        dto.setId(enquiry.getId());
        dto.setCustomerName(enquiry.getCustomerName());
        dto.setCustomerEmail(enquiry.getCustomerEmail());
        dto.setMessage(enquiry.getMessage());
        dto.setStatus(enquiry.getStatus());
        lenient().when(modelMapper.map(eq(enquiry), eq(EnquiryDto.class))).thenReturn(dto);
    }

    @BeforeEach
    void setUp() {
        // Setup is minimal; each test stubs what it needs
    }

    @Test
    void getAllEnquiries_usesJoinFetchVariant_notFindAll() {
        Enquiry enquiry = createTestEnquiry(1L);
        setupMapper(enquiry);
        Page<Enquiry> page = new PageImpl<>(List.of(enquiry));
        when(enquiryRepository.findAllWithCustomerVehicleShop(any(Pageable.class))).thenReturn(page);

        Page<EnquiryDto> result = enquiryService.getAllEnquiries(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(enquiryRepository).findAllWithCustomerVehicleShop(any(Pageable.class));
        verify(enquiryRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getEnquiriesByCustomer_usesJoinFetchVariant_notFindByCustomerId() {
        Enquiry enquiry = createTestEnquiry(1L);
        setupMapper(enquiry);
        Page<Enquiry> page = new PageImpl<>(List.of(enquiry));
        when(enquiryRepository.findByCustomerIdWithCustomerVehicleShop(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<EnquiryDto> result = enquiryService.getEnquiriesByCustomer(1L, 0, 10);

        assertNotNull(result);
        verify(enquiryRepository).findByCustomerIdWithCustomerVehicleShop(eq(1L), any(Pageable.class));
        verify(enquiryRepository, never()).findByCustomerId(anyLong(), any(Pageable.class));
    }

    @Test
    void getEnquiriesByShop_usesJoinFetchVariant_notFindByShopId() {
        Enquiry enquiry = createTestEnquiry(1L);
        setupMapper(enquiry);
        Page<Enquiry> page = new PageImpl<>(List.of(enquiry));
        when(enquiryRepository.findByShopIdWithCustomerVehicleShop(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<EnquiryDto> result = enquiryService.getEnquiriesByShop(1L, 0, 10);

        assertNotNull(result);
        verify(enquiryRepository).findByShopIdWithCustomerVehicleShop(eq(1L), any(Pageable.class));
        verify(enquiryRepository, never()).findByShopId(anyLong(), any(Pageable.class));
    }

    @Test
    void getEnquiriesByVehicle_usesJoinFetchVariant_notFindByVehicleId() {
        Enquiry enquiry = createTestEnquiry(1L);
        setupMapper(enquiry);
        Page<Enquiry> page = new PageImpl<>(List.of(enquiry));
        when(enquiryRepository.findByVehicleIdWithCustomerVehicleShop(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<EnquiryDto> result = enquiryService.getEnquiriesByVehicle(1L, 0, 10);

        assertNotNull(result);
        verify(enquiryRepository).findByVehicleIdWithCustomerVehicleShop(eq(1L), any(Pageable.class));
        verify(enquiryRepository, never()).findByVehicleId(anyLong(), any(Pageable.class));
    }

    @Test
    void getEnquiriesByStatus_usesJoinFetchVariant_notFindByStatus() {
        Enquiry enquiry = createTestEnquiry(1L);
        setupMapper(enquiry);
        Page<Enquiry> page = new PageImpl<>(List.of(enquiry));
        when(enquiryRepository.findByStatusWithCustomerVehicleShop(eq(EnquiryStatus.PENDING), any(Pageable.class))).thenReturn(page);

        Page<EnquiryDto> result = enquiryService.getEnquiriesByStatus(EnquiryStatus.PENDING, 0, 10);

        assertNotNull(result);
        verify(enquiryRepository).findByStatusWithCustomerVehicleShop(eq(EnquiryStatus.PENDING), any(Pageable.class));
        verify(enquiryRepository, never()).findByStatus(any(EnquiryStatus.class), any(Pageable.class));
    }

    @Test
    void getPendingEnquiriesByShop_usesJoinFetchVariant_notFindByShopIdAndStatus() {
        Enquiry enquiry = createTestEnquiry(1L);
        setupMapper(enquiry);
        when(enquiryRepository.findByShopIdAndStatusWithCustomerVehicleShop(eq(1L), eq(EnquiryStatus.PENDING)))
                .thenReturn(List.of(enquiry));

        List<EnquiryDto> result = enquiryService.getPendingEnquiriesByShop(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(enquiryRepository).findByShopIdAndStatusWithCustomerVehicleShop(eq(1L), eq(EnquiryStatus.PENDING));
        verify(enquiryRepository, never()).findByShopIdAndStatus(anyLong(), any(EnquiryStatus.class));
    }

    @Test
    void searchEnquiries_usesJoinFetchVariant_notSearchByKeyword() {
        Enquiry enquiry = createTestEnquiry(1L);
        setupMapper(enquiry);
        Page<Enquiry> page = new PageImpl<>(List.of(enquiry));
        when(enquiryRepository.searchByKeywordWithCustomerVehicleShop(eq("test"), any(Pageable.class))).thenReturn(page);

        Page<EnquiryDto> result = enquiryService.searchEnquiries("test", 0, 10);

        assertNotNull(result);
        verify(enquiryRepository).searchByKeywordWithCustomerVehicleShop(eq("test"), any(Pageable.class));
        verify(enquiryRepository, never()).searchByKeyword(anyString(), any(Pageable.class));
    }

    @Test
    void getEnquiryById_usesJoinFetchVariant_notFindById() {
        Enquiry enquiry = createTestEnquiry(1L);
        setupMapper(enquiry);
        when(enquiryRepository.findByIdWithCustomerVehicleShop(eq(1L))).thenReturn(Optional.of(enquiry));

        Optional<EnquiryDto> result = enquiryService.getEnquiryById(1L);

        assertTrue(result.isPresent());
        verify(enquiryRepository).findByIdWithCustomerVehicleShop(eq(1L));
        verify(enquiryRepository, never()).findById(anyLong());
    }

    @Test
    void createEnquiry_doesNotCallVehicleRepositoryTwice() {
        EnquiryDto dto = EnquiryDto.builder()
                .customerName("Test Customer")
                .customerEmail("test@test.com")
                .customerPhone("1234567890")
                .message("Test message")
                .vehicleId(1L)
                .build();

        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setName("Test Shop");
        vehicle.setShop(shop);

        Enquiry savedEnquiry = createTestEnquiry(1L);
        setupMapper(savedEnquiry);

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(savedEnquiry);

        enquiryService.createEnquiry(dto);

        // Should call vehicleRepository.findById only ONCE, not twice
        verify(vehicleRepository, times(1)).findById(1L);
    }
}
