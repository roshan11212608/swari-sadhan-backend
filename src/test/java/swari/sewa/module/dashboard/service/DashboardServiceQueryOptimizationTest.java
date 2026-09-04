package swari.sewa.module.dashboard.service;

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
import swari.sewa.common.enums.ShopStatus;
import swari.sewa.common.enums.UserRole;
import swari.sewa.common.enums.VehicleStatus;
import swari.sewa.module.dashboard.dto.CredentialsDto;
import swari.sewa.module.dashboard.service.impl.DashboardServiceImpl;
import swari.sewa.module.enquiry.entity.Enquiry;
import swari.sewa.module.enquiry.repository.EnquiryRepository;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DashboardServiceImpl query optimization changes.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>getCredentials uses DB-level filtering and pagination instead of
 *       loading ALL users and ALL shop owners into memory.</li>
 *   <li>getEnquiryReports uses JOIN FETCH variant instead of findAll
 *       to avoid N+1 on customer and vehicle lazy loads.</li>
 *   <li>getCredentialById, resetUserPassword, toggleCredentialStatus
 *       use findById directly instead of existsById + findById.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceQueryOptimizationTest {

    @Mock private ShopOwnerRepository shopOwnerRepository;
    @Mock private UserRepository userRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private EnquiryRepository enquiryRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private ShopOwner createTestShopOwner(Long id) {
        ShopOwner shopOwner = new ShopOwner();
        shopOwner.setId(id);
        shopOwner.setFirstName("Owner");
        shopOwner.setLastName(String.valueOf(id));
        shopOwner.setEmail("owner" + id + "@test.com");
        shopOwner.setPhone("1234567890");
        shopOwner.setCompanyName("Test Shop " + id);
        shopOwner.setActive(true);
        return shopOwner;
    }

    private User createTestUser(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setFirstName("User");
        user.setLastName(String.valueOf(id));
        user.setEmail("user" + id + "@test.com");
        user.setPhone("1234567890");
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    // ── getCredentials: DB-level filtering and pagination ──

    @Test
    void getCredentials_shopOwnerType_usesShopOwnerRepository_notFindAllUsers() {
        Page<ShopOwner> shopOwnerPage = new PageImpl<>(List.of(createTestShopOwner(1L)));
        when(shopOwnerRepository.findAll(any(Pageable.class))).thenReturn(shopOwnerPage);

        Page<CredentialsDto> result = dashboardService.getCredentials(
                PageRequest.of(0, 10), "SHOP_OWNER", null, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        // Should NOT load all users
        verify(userRepository, never()).findAll();
        verify(shopOwnerRepository).findAll(any(Pageable.class));
    }

    @Test
    void getCredentials_shopOwnerType_withSearch_usesSearchByKeyword() {
        Page<ShopOwner> shopOwnerPage = new PageImpl<>(List.of(createTestShopOwner(1L)));
        when(shopOwnerRepository.searchByKeyword(eq("test"), any(Pageable.class))).thenReturn(shopOwnerPage);

        Page<CredentialsDto> result = dashboardService.getCredentials(
                PageRequest.of(0, 10), "SHOP_OWNER", null, "test");

        assertNotNull(result);
        verify(shopOwnerRepository).searchByKeyword(eq("test"), any(Pageable.class));
        verify(userRepository, never()).findAll();
    }

    @Test
    void getCredentials_shopOwnerType_withStatus_usesFindByActive() {
        Page<ShopOwner> shopOwnerPage = new PageImpl<>(List.of(createTestShopOwner(1L)));
        when(shopOwnerRepository.findByActive(eq(true), any(Pageable.class))).thenReturn(shopOwnerPage);

        Page<CredentialsDto> result = dashboardService.getCredentials(
                PageRequest.of(0, 10), "SHOP_OWNER", "ACTIVE", null);

        assertNotNull(result);
        verify(shopOwnerRepository).findByActive(eq(true), any(Pageable.class));
        verify(userRepository, never()).findAll();
    }

    @Test
    void getCredentials_shopOwnerType_withSearchAndStatus_usesSearchByKeywordAndActive() {
        Page<ShopOwner> shopOwnerPage = new PageImpl<>(List.of(createTestShopOwner(1L)));
        when(shopOwnerRepository.searchByKeywordAndActive(eq("test"), eq(false), any(Pageable.class)))
                .thenReturn(shopOwnerPage);

        Page<CredentialsDto> result = dashboardService.getCredentials(
                PageRequest.of(0, 10), "SHOP_OWNER", "INACTIVE", "test");

        assertNotNull(result);
        verify(shopOwnerRepository).searchByKeywordAndActive(eq("test"), eq(false), any(Pageable.class));
        verify(userRepository, never()).findAll();
    }

    @Test
    void getCredentials_superadminType_usesUserRepositoryFindByRole() {
        Page<User> userPage = new PageImpl<>(List.of(createTestUser(1L, UserRole.SUPERADMIN)));
        when(userRepository.findByRole(eq(UserRole.SUPERADMIN), any(Pageable.class))).thenReturn(userPage);

        Page<CredentialsDto> result = dashboardService.getCredentials(
                PageRequest.of(0, 10), "SUPERADMIN", null, null);

        assertNotNull(result);
        verify(userRepository).findByRole(eq(UserRole.SUPERADMIN), any(Pageable.class));
        verify(userRepository, never()).findAll();
        verify(shopOwnerRepository, never()).findAll();
    }

    @Test
    void getCredentials_publicUserType_usesUserRepositoryFindByRole() {
        Page<User> userPage = new PageImpl<>(List.of(createTestUser(1L, UserRole.PUBLIC)));
        when(userRepository.findByRole(eq(UserRole.PUBLIC), any(Pageable.class))).thenReturn(userPage);

        Page<CredentialsDto> result = dashboardService.getCredentials(
                PageRequest.of(0, 10), "PUBLIC_USER", null, null);

        assertNotNull(result);
        verify(userRepository).findByRole(eq(UserRole.PUBLIC), any(Pageable.class));
        verify(userRepository, never()).findAll();
        verify(shopOwnerRepository, never()).findAll();
    }

    @Test
    void getCredentials_nullUserType_queriesEachTableWithPagination_notFindAll() {
        Page<User> superadminPage = new PageImpl<>(List.of(createTestUser(1L, UserRole.SUPERADMIN)));
        Page<ShopOwner> shopOwnerPage = new PageImpl<>(List.of(createTestShopOwner(2L)));
        Page<User> publicUserPage = new PageImpl<>(List.of(createTestUser(3L, UserRole.PUBLIC)));

        when(userRepository.findByRole(eq(UserRole.SUPERADMIN), any(Pageable.class))).thenReturn(superadminPage);
        when(shopOwnerRepository.findAll(any(Pageable.class))).thenReturn(shopOwnerPage);
        when(userRepository.findByRole(eq(UserRole.PUBLIC), any(Pageable.class))).thenReturn(publicUserPage);

        Page<CredentialsDto> result = dashboardService.getCredentials(
                PageRequest.of(0, 10), null, null, null);

        assertNotNull(result);
        // Should use paginated queries, not findAll on users
        verify(userRepository, never()).findAll();
        verify(shopOwnerRepository).findAll(any(Pageable.class));
    }

    // ── getEnquiryReports: JOIN FETCH variant ──

    @Test
    void getEnquiryReports_usesJoinFetchVariant_notFindAll() {
        Enquiry enquiry = new Enquiry();
        enquiry.setId(1L);
        enquiry.setStatus(EnquiryStatus.PENDING);
        User customer = createTestUser(1L, UserRole.PUBLIC);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setTitle("Test Vehicle");
        enquiry.setCustomer(customer);
        enquiry.setVehicle(vehicle);

        Page<Enquiry> enquiryPage = new PageImpl<>(List.of(enquiry));
        when(enquiryRepository.findAllWithCustomerVehicleShop(any(Pageable.class))).thenReturn(enquiryPage);

        Page<Object> result = dashboardService.getEnquiryReports(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(enquiryRepository).findAllWithCustomerVehicleShop(any(Pageable.class));
        verify(enquiryRepository, never()).findAll(any(Pageable.class));
    }

    // ── getCredentialById: no redundant existsById + findById ──

    @Test
    void getCredentialById_findsShopOwner_usesFindByIdOnly_notExistsById() {
        ShopOwner shopOwner = createTestShopOwner(1L);
        when(shopOwnerRepository.findById(1L)).thenReturn(Optional.of(shopOwner));

        CredentialsDto result = dashboardService.getCredentialById(1L);

        assertNotNull(result);
        assertEquals("SHOP_OWNER", result.getUserType());
        verify(shopOwnerRepository).findById(1L);
        verify(shopOwnerRepository, never()).existsById(anyLong());
    }

    @Test
    void getCredentialById_findsUser_usesFindByIdOnly_notExistsById() {
        User user = createTestUser(1L, UserRole.PUBLIC);
        when(shopOwnerRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        CredentialsDto result = dashboardService.getCredentialById(1L);

        assertNotNull(result);
        assertEquals("PUBLIC_USER", result.getUserType());
        verify(shopOwnerRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsById(anyLong());
    }

    @Test
    void getCredentialById_throwsWhenNotFound() {
        when(shopOwnerRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> dashboardService.getCredentialById(1L));
    }

    // ── resetUserPassword: no redundant existsById + findById ──

    @Test
    void resetUserPassword_findsShopOwner_usesFindByIdOnly() {
        ShopOwner shopOwner = createTestShopOwner(1L);
        when(shopOwnerRepository.findById(1L)).thenReturn(Optional.of(shopOwner));

        String result = dashboardService.resetUserPassword(1L);

        assertNotNull(result);
        verify(shopOwnerRepository).findById(1L);
        verify(shopOwnerRepository, never()).existsById(anyLong());
    }

    @Test
    void resetUserPassword_findsUser_usesFindByIdOnly() {
        User user = createTestUser(1L, UserRole.PUBLIC);
        when(shopOwnerRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String result = dashboardService.resetUserPassword(1L);

        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsById(anyLong());
    }

    // ── toggleCredentialStatus: no redundant existsById + findById ──

    @Test
    void toggleCredentialStatus_findsShopOwner_usesFindByIdOnly() {
        ShopOwner shopOwner = createTestShopOwner(1L);
        when(shopOwnerRepository.findById(1L)).thenReturn(Optional.of(shopOwner));

        dashboardService.toggleCredentialStatus(1L);

        verify(shopOwnerRepository).findById(1L);
        verify(shopOwnerRepository, never()).existsById(anyLong());
        verify(shopOwnerRepository).save(any(ShopOwner.class));
    }

    @Test
    void toggleCredentialStatus_findsUser_usesFindByIdOnly() {
        User user = createTestUser(1L, UserRole.PUBLIC);
        when(shopOwnerRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        dashboardService.toggleCredentialStatus(1L);

        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsById(anyLong());
        verify(userRepository).save(any(User.class));
    }
}
