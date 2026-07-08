package swari.sewa.module.vehicle.service.impl;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.common.enums.ApplicationStatus;
import swari.sewa.module.vehicle.dto.SellVehicleApplicationDto;
import swari.sewa.module.vehicle.entity.SellVehicleApplication;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.vehicle.repository.SellVehicleApplicationRepository;
import swari.sewa.module.vehicle.repository.VehicleRepository;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.vehicle.service.SellVehicleApplicationService;
import swari.sewa.common.enums.VehicleStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SellVehicleApplicationServiceImpl implements SellVehicleApplicationService {

    private final SellVehicleApplicationRepository applicationRepository;
    private final VehicleRepository vehicleRepository;
    private final ShopRepository shopRepository;
    private final ModelMapper modelMapper;

    @Override
    public SellVehicleApplicationDto createApplication(SellVehicleApplicationDto applicationDto, Long vehicleId, Long shopId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));

        // Create application manually instead of using ModelMapper to avoid deserialization issues
        SellVehicleApplication application = new SellVehicleApplication();
        application.setVehicle(vehicle);
        application.setShop(shop);
        application.setStatus(ApplicationStatus.PENDING);

        // Customer Information
        application.setCustomerName(applicationDto.getCustomerName());
        application.setCustomerParentName(applicationDto.getCustomerParentName());
        application.setCustomerPhone(applicationDto.getCustomerPhone());
        application.setCustomerEmail(applicationDto.getCustomerEmail());
        application.setCustomerAddress(applicationDto.getCustomerAddress());
        application.setCustomerCitizenshipNumber(applicationDto.getCustomerCitizenshipNumber());

        // Customer Photos/Documents
        application.setCustomerPhoto(applicationDto.getCustomerPhoto());
        application.setCitizenshipFrontPhoto(applicationDto.getCitizenshipFrontPhoto());
        application.setCitizenshipBackPhoto(applicationDto.getCitizenshipBackPhoto());

        // Application Details
        application.setApplicationDate(applicationDto.getApplicationDate());
        application.setOfferedPrice(applicationDto.getOfferedPrice());
        application.setOfferedPriceInWords(applicationDto.getOfferedPriceInWords());
        application.setPaymentMethod(applicationDto.getPaymentMethod());
        application.setDownPayment(applicationDto.getDownPayment());
        application.setFinancingRequired(applicationDto.getFinancingRequired());
        application.setFinancingBank(applicationDto.getFinancingBank());
        application.setFinancingAmount(applicationDto.getFinancingAmount());

        // Sales Information
        application.setSalesManName(applicationDto.getSalesManName());

        // Additional Information
        application.setCustomerOccupation(applicationDto.getCustomerOccupation());
        application.setCustomerIncome(applicationDto.getCustomerIncome());
        application.setReferenceName(applicationDto.getReferenceName());
        application.setReferencePhone(applicationDto.getReferencePhone());
        application.setReferenceRelation(applicationDto.getReferenceRelation());

        // Documents
        application.setCitizenshipCopyProvided(applicationDto.getCitizenshipCopyProvided());
        application.setPhotoProvided(applicationDto.getPhotoProvided());
        application.setAddressProofProvided(applicationDto.getAddressProofProvided());
        application.setIncomeProofProvided(applicationDto.getIncomeProofProvided());

        // Terms and Conditions
        application.setTermsAccepted(applicationDto.getTermsAccepted());
        application.setBackgroundCheckConsent(applicationDto.getBackgroundCheckConsent());

        // Notes
        application.setNotes(applicationDto.getNotes());

        SellVehicleApplication savedApplication = applicationRepository.save(application);
        return mapToDto(savedApplication);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SellVehicleApplicationDto> getApplicationById(Long id, Long shopId) {
        return applicationRepository.findByIdAndShopId(id, shopId)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SellVehicleApplicationDto> getApplicationsByShop(Long shopId, Pageable pageable) {
        return applicationRepository.findByShopId(shopId, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SellVehicleApplicationDto> getApplicationsByShopAndStatus(Long shopId, ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByShopIdAndStatus(shopId, status, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SellVehicleApplicationDto> getApplicationsByVehicle(Long vehicleId, Pageable pageable) {
        return applicationRepository.findByVehicleId(vehicleId, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellVehicleApplicationDto> getApplicationsByVehicleAndShop(Long vehicleId, Long shopId) {
        return applicationRepository.findByVehicleIdAndShopId(vehicleId, shopId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public SellVehicleApplicationDto updateApplicationStatus(Long id, ApplicationStatus status, Long shopId, String notes) {
        SellVehicleApplication application = applicationRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        application.setStatus(status);
        if (notes != null) {
            application.setNotes(notes);
        }

        // Update vehicle status based on application status
        Vehicle vehicle = application.getVehicle();
        switch (status) {
            case APPROVED:
                vehicle.setStatus(VehicleStatus.SOLD);
                vehicleRepository.save(vehicle);
                break;
            case REJECTED:
            case CANCELLED:
                // Check if there are any other pending applications for this vehicle
                List<SellVehicleApplication> pendingApplications = applicationRepository.findByVehicleIdAndShopId(vehicle.getId(), shopId)
                        .stream()
                        .filter(app -> app.getStatus() == ApplicationStatus.PENDING && !app.getId().equals(id))
                        .collect(Collectors.toList());
                
                if (pendingApplications.isEmpty()) {
                    // No other pending applications, revert vehicle to ACTIVE
                    vehicle.setStatus(VehicleStatus.ACTIVE);
                    vehicleRepository.save(vehicle);
                }
                break;
            case COMPLETED:
                vehicle.setStatus(VehicleStatus.SOLD);
                vehicleRepository.save(vehicle);
                break;
            default:
                // For PENDING or UNDER_REVIEW, keep vehicle as PENDING_SALE
                break;
        }

        SellVehicleApplication updatedApplication = applicationRepository.save(application);
        return mapToDto(updatedApplication);
    }

    @Override
    public SellVehicleApplicationDto updateApplication(Long id, SellVehicleApplicationDto applicationDto, Long shopId) {
        SellVehicleApplication application = applicationRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        modelMapper.map(applicationDto, application);
        
        SellVehicleApplication updatedApplication = applicationRepository.save(application);
        return mapToDto(updatedApplication);
    }

    @Override
    public void deleteApplication(Long id, Long shopId) {
        SellVehicleApplication application = applicationRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        applicationRepository.delete(application);
    }

    @Override
    @Transactional(readOnly = true)
    public long countApplicationsByShop(Long shopId) {
        return applicationRepository.countByShopId(shopId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countApplicationsByShopAndStatus(Long shopId, ApplicationStatus status) {
        return applicationRepository.countByShopIdAndStatus(shopId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SellVehicleApplicationDto> searchApplicationsByCustomerEmail(Long shopId, String email, Pageable pageable) {
        return applicationRepository.findByShopIdAndCustomerEmailContaining(shopId, email, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SellVehicleApplicationDto> searchApplicationsByCustomerPhone(Long shopId, String phone, Pageable pageable) {
        return applicationRepository.findByShopIdAndCustomerPhoneContaining(shopId, phone, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SellVehicleApplicationDto> searchApplicationsByCustomerName(Long shopId, String name, Pageable pageable) {
        return applicationRepository.findByShopIdAndCustomerNameContaining(shopId, name, pageable)
                .map(this::mapToDto);
    }

    private SellVehicleApplicationDto mapToDto(SellVehicleApplication application) {
        SellVehicleApplicationDto dto = modelMapper.map(application, SellVehicleApplicationDto.class);
        dto.setVehicleId(application.getVehicle().getId());
        dto.setVehicleTitle(application.getVehicle().getTitle());
        dto.setVehicleBrand(application.getVehicle().getBrandName());
        dto.setVehicleModel(application.getVehicle().getModelName());
        dto.setVehiclePrice(application.getVehicle().getPrice());
        dto.setShopId(application.getShop().getId());
        return dto;
    }
}
