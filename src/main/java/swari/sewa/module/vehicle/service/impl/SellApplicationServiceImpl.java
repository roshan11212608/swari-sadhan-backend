package swari.sewa.module.vehicle.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swari.sewa.module.vehicle.dto.SellApplicationDto;
import swari.sewa.module.vehicle.dto.SellVehicleApplicationDto;
import swari.sewa.module.vehicle.entity.SellApplication;
import swari.sewa.module.vehicle.repository.SellApplicationRepository;
import swari.sewa.module.vehicle.service.SellApplicationService;

import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SellApplicationServiceImpl implements SellApplicationService {

    private final SellApplicationRepository sellApplicationRepository;

    @Override
    public SellApplicationDto createSellApplication(SellVehicleApplicationDto sellApplicationDto) {
        SellApplication sellApplication = SellApplication.builder()
                .vehicleId(sellApplicationDto.getVehicleId())
                .customerName(sellApplicationDto.getCustomerName())
                .customerParentName(sellApplicationDto.getCustomerParentName())
                .customerPhone(sellApplicationDto.getCustomerPhone())
                .customerEmail(sellApplicationDto.getCustomerEmail())
                .customerAddress(sellApplicationDto.getCustomerAddress())
                .customerCitizenshipNumber(sellApplicationDto.getCustomerCitizenshipNumber())
                .citizenshipFrontPhoto(sellApplicationDto.getCitizenshipFrontPhoto())
                .citizenshipBackPhoto(sellApplicationDto.getCitizenshipBackPhoto())
                .customerPhoto(sellApplicationDto.getCustomerPhoto())
                .applicationDate(sellApplicationDto.getApplicationDate())
                .offeredPrice(sellApplicationDto.getOfferedPrice())
                .offeredPriceInWords(sellApplicationDto.getOfferedPriceInWords())
                .paymentMethod(sellApplicationDto.getPaymentMethod())
                .downPayment(sellApplicationDto.getDownPayment())
                .financingRequired(sellApplicationDto.getFinancingRequired())
                .financingBank(sellApplicationDto.getFinancingBank())
                .status(sellApplicationDto.getStatus() != null ? sellApplicationDto.getStatus().name() : "PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        SellApplication savedApplication = sellApplicationRepository.save(sellApplication);
        
        return convertToDto(savedApplication);
    }

    @Override
    public List<SellApplicationDto> getSellApplicationsByVehicleId(Long vehicleId) {
        List<SellApplication> applications = sellApplicationRepository.findByVehicleId(vehicleId);
        return applications.stream()
                .map(this::convertToDto)
                .toList();
    }

    private SellApplicationDto convertToDto(SellApplication sellApplication) {
        return SellApplicationDto.builder()
                .id(sellApplication.getId())
                .vehicleId(sellApplication.getVehicleId())
                .customerName(sellApplication.getCustomerName())
                .customerParentName(sellApplication.getCustomerParentName())
                .customerPhone(sellApplication.getCustomerPhone())
                .customerEmail(sellApplication.getCustomerEmail())
                .customerAddress(sellApplication.getCustomerAddress())
                .customerCitizenshipNumber(sellApplication.getCustomerCitizenshipNumber())
                .citizenshipFrontPhoto(sellApplication.getCitizenshipFrontPhoto())
                .citizenshipBackPhoto(sellApplication.getCitizenshipBackPhoto())
                .customerPhoto(sellApplication.getCustomerPhoto())
                .applicationDate(sellApplication.getApplicationDate())
                .offeredPrice(sellApplication.getOfferedPrice())
                .offeredPriceInWords(sellApplication.getOfferedPriceInWords())
                .paymentMethod(sellApplication.getPaymentMethod() != null ? sellApplication.getPaymentMethod().name() : null)
                .downPayment(sellApplication.getDownPayment())
                .financingRequired(sellApplication.getFinancingRequired())
                .financingBank(sellApplication.getFinancingBank())
                .status(sellApplication.getStatus())
                .createdAt(sellApplication.getCreatedAt())
                .updatedAt(sellApplication.getUpdatedAt())
                .build();
    }
}
