package swari.sewa.module.superadmin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.publicuser.model.User;
import swari.sewa.module.publicuser.repository.UserRepository;
import swari.sewa.module.shopowner.model.Shop;
import swari.sewa.module.shopowner.model.ShopOwner;
import swari.sewa.module.shopowner.repository.ShopOwnerRepository;
import swari.sewa.module.shopowner.repository.ShopRepository;
import swari.sewa.module.shopowner.model.Vehicle;
import swari.sewa.module.shopowner.repository.VehicleRepository;
import swari.sewa.module.publicuser.model.Enquiry;
import swari.sewa.module.publicuser.repository.EnquiryRepository;
import swari.sewa.module.superadmin.dto.DashboardStatsDto;
import swari.sewa.module.superadmin.dto.ShopOwnerDto;
import swari.sewa.module.superadmin.dto.UserManagementDto;
import swari.sewa.common.enums.UserRole;
import swari.sewa.common.enums.ShopStatus;
import swari.sewa.common.enums.VehicleStatus;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SuperAdminServiceImpl implements SuperAdminService {

    private final ShopOwnerRepository shopOwnerRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final VehicleRepository vehicleRepository;
    private final EnquiryRepository enquiryRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long totalShopOwners = shopOwnerRepository.count();
        long activeShopOwners = shopOwnerRepository.countByActive(true);
        long totalShops = shopRepository.count();
        long activeShops = shopRepository.countByStatus(ShopStatus.ACTIVE);
        long totalVehicles = vehicleRepository.count();
        long activeVehicles = vehicleRepository.countByStatus(VehicleStatus.ACTIVE);
        long totalUsers = userRepository.countByRole(UserRole.PUBLIC);
        long totalEnquiries = enquiryRepository.count();
        long pendingEnquiries = enquiryRepository.countByStatus(swari.sewa.common.enums.EnquiryStatus.PENDING);

        return DashboardStatsDto.builder()
                .totalShopOwners(totalShopOwners)
                .activeShopOwners(activeShopOwners)
                .totalShops(totalShops)
                .activeShops(activeShops)
                .totalVehicles(totalVehicles)
                .activeVehicles(activeVehicles)
                .totalUsers(totalUsers)
                .totalEnquiries(totalEnquiries)
                .pendingEnquiries(pendingEnquiries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopOwnerDto> getShopOwners(Pageable pageable) {
        return shopOwnerRepository.findAll(pageable)
                .map(shopOwner -> ShopOwnerDto.builder()
                        .id(shopOwner.getId())
                        .firstName(shopOwner.getFirstName())
                        .lastName(shopOwner.getLastName())
                        .email(shopOwner.getEmail())
                        .phone(shopOwner.getPhone())
                        .companyName(shopOwner.getCompanyName())
                        .active(shopOwner.isActive())
                        .createdAt(shopOwner.getCreatedAt())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserManagementDto> getUsers(Pageable pageable) {
        return userRepository.findByRole(UserRole.PUBLIC, pageable)
                .map(user -> UserManagementDto.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .active(user.isActive())
                        .createdAt(user.getCreatedAt())
                        .build());
    }

    @Override
    public void activateShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(true);
        shopOwnerRepository.save(shopOwner);
    }

    @Override
    public void deactivateShopOwner(Long id) {
        ShopOwner shopOwner = shopOwnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        shopOwner.setActive(false);
        shopOwnerRepository.save(shopOwner);
    }

    @Override
    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getReportsSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRevenue", 0L); // TODO: Implement revenue calculation
        summary.put("monthlyRevenue", 0L); // TODO: Implement monthly revenue
        summary.put("totalTransactions", 0L); // TODO: Implement transaction count
        summary.put("activeSubscriptions", 0L); // TODO: Implement subscription count
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getVehicleReports(Pageable pageable) {
        // TODO: Implement detailed vehicle reports
        return vehicleRepository.findAll(pageable)
                .map(vehicle -> {
                    Map<String, Object> report = new HashMap<>();
                    report.put("id", vehicle.getId());
                    report.put("title", vehicle.getTitle());
                    report.put("status", vehicle.getStatus());
                    report.put("views", vehicle.getViewCount());
                    report.put("contacts", vehicle.getContactCount());
                    report.put("createdAt", vehicle.getCreatedAt());
                    return report;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getEnquiryReports(Pageable pageable) {
        return enquiryRepository.findAll(pageable)
                .map(enquiry -> {
                    Map<String, Object> report = new HashMap<>();
                    report.put("id", enquiry.getId());
                    report.put("customerName", enquiry.getCustomer().getFirstName() + " " + enquiry.getCustomer().getLastName());
                    report.put("vehicleTitle", enquiry.getVehicle().getTitle());
                    report.put("status", enquiry.getStatus());
                    report.put("createdAt", enquiry.getCreatedAt());
                    return report;
                });
    }
}
