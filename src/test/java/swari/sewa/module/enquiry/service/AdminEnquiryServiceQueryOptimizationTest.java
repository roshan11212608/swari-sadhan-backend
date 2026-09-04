package swari.sewa.module.enquiry.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import swari.sewa.common.enums.EnquiryStatus;
import swari.sewa.module.enquiry.entity.Enquiry;
import swari.sewa.module.enquiry.repository.EnquiryRepository;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.enquiry.service.impl.AdminEnquiryServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminEnquiryServiceImpl query optimization changes.
 *
 * <p>Verifies that admin list and detail endpoints use JOIN FETCH repository
 * variants instead of plain findAll/findByStatus that trigger N+1 lazy loads
 * on customer, vehicle, and shop relationships.
 */
@ExtendWith(MockitoExtension.class)
class AdminEnquiryServiceQueryOptimizationTest {

    @Mock private EnquiryRepository enquiryRepository;

    @InjectMocks
    private AdminEnquiryServiceImpl adminEnquiryService;

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

    @Test
    void getAllEnquiries_noFilters_usesJoinFetchVariant() {
        Enquiry enquiry = createTestEnquiry(1L);
        Page<Enquiry> page = new PageImpl<>(List.of(enquiry));
        when(enquiryRepository.findAllWithCustomerVehicleShop(any(Pageable.class))).thenReturn(page);

        Page<Object> result = adminEnquiryService.getAllEnquiries(PageRequest.of(0, 10), null, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(enquiryRepository).findAllWithCustomerVehicleShop(any(Pageable.class));
        verify(enquiryRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllEnquiries_withStatus_usesJoinFetchVariant() {
        Enquiry enquiry = createTestEnquiry(1L);
        Page<Enquiry> page = new PageImpl<>(List.of(enquiry));
        when(enquiryRepository.findByStatusWithCustomerVehicleShop(eq(EnquiryStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);

        Page<Object> result = adminEnquiryService.getAllEnquiries(PageRequest.of(0, 10), "PENDING", null);

        assertNotNull(result);
        verify(enquiryRepository).findByStatusWithCustomerVehicleShop(eq(EnquiryStatus.PENDING), any(Pageable.class));
        verify(enquiryRepository, never()).findByStatus(any(EnquiryStatus.class), any(Pageable.class));
    }

    @Test
    void getAllEnquiries_withSearch_usesJoinFetchVariant() {
        Enquiry enquiry = createTestEnquiry(1L);
        Page<Enquiry> page = new PageImpl<>(List.of(enquiry));
        when(enquiryRepository.searchByCustomerNameWithCustomerVehicleShop(eq("test"), eq("test"), any(Pageable.class)))
                .thenReturn(page);

        Page<Object> result = adminEnquiryService.getAllEnquiries(PageRequest.of(0, 10), null, "test");

        assertNotNull(result);
        verify(enquiryRepository).searchByCustomerNameWithCustomerVehicleShop(eq("test"), eq("test"), any(Pageable.class));
    }

    @Test
    void getPendingEnquiries_usesJoinFetchVariant() {
        Enquiry enquiry = createTestEnquiry(1L);
        Page<Enquiry> page = new PageImpl<>(List.of(enquiry));
        when(enquiryRepository.findByStatusWithCustomerVehicleShop(eq(EnquiryStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);

        Page<Object> result = adminEnquiryService.getPendingEnquiries(PageRequest.of(0, 10));

        assertNotNull(result);
        verify(enquiryRepository).findByStatusWithCustomerVehicleShop(eq(EnquiryStatus.PENDING), any(Pageable.class));
        verify(enquiryRepository, never()).findByStatus(any(EnquiryStatus.class), any(Pageable.class));
    }

    @Test
    void getEnquiryById_usesJoinFetchVariant_notFindById() {
        Enquiry enquiry = createTestEnquiry(1L);
        when(enquiryRepository.findByIdWithCustomerVehicleShop(eq(1L))).thenReturn(Optional.of(enquiry));

        Object result = adminEnquiryService.getEnquiryById(1L);

        assertNotNull(result);
        verify(enquiryRepository).findByIdWithCustomerVehicleShop(eq(1L));
        verify(enquiryRepository, never()).findById(anyLong());
    }
}
