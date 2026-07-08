package swari.sewa.module.vehicle.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swari.sewa.module.vehicle.dto.SellVehicleApplicationDto;
import swari.sewa.common.enums.ApplicationStatus;

import java.util.List;
import java.util.Optional;

public interface SellVehicleApplicationService {

    SellVehicleApplicationDto createApplication(SellVehicleApplicationDto applicationDto, Long vehicleId, Long shopId);

    Optional<SellVehicleApplicationDto> getApplicationById(Long id, Long shopId);

    Page<SellVehicleApplicationDto> getApplicationsByShop(Long shopId, Pageable pageable);

    Page<SellVehicleApplicationDto> getApplicationsByShopAndStatus(Long shopId, ApplicationStatus status, Pageable pageable);

    Page<SellVehicleApplicationDto> getApplicationsByVehicle(Long vehicleId, Pageable pageable);

    List<SellVehicleApplicationDto> getApplicationsByVehicleAndShop(Long vehicleId, Long shopId);

    SellVehicleApplicationDto updateApplicationStatus(Long id, ApplicationStatus status, Long shopId, String notes);

    SellVehicleApplicationDto updateApplication(Long id, SellVehicleApplicationDto applicationDto, Long shopId);

    void deleteApplication(Long id, Long shopId);

    long countApplicationsByShop(Long shopId);

    long countApplicationsByShopAndStatus(Long shopId, ApplicationStatus status);

    Page<SellVehicleApplicationDto> searchApplicationsByCustomerEmail(Long shopId, String email, Pageable pageable);

    Page<SellVehicleApplicationDto> searchApplicationsByCustomerPhone(Long shopId, String phone, Pageable pageable);

    Page<SellVehicleApplicationDto> searchApplicationsByCustomerName(Long shopId, String name, Pageable pageable);
}
