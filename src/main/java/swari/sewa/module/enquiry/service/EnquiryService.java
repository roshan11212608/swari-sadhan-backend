package swari.sewa.module.enquiry.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import swari.sewa.module.enquiry.dto.EnquiryDto;
import swari.sewa.module.enquiry.dto.EnquiryMessageDto;
import swari.sewa.common.enums.EnquiryStatus;

public interface EnquiryService {
    
    EnquiryDto createEnquiry(EnquiryDto enquiryDto);
    
    Optional<EnquiryDto> getEnquiryById(Long id);
    
    Page<EnquiryDto> getAllEnquiries(int page, int size);
    
    Page<EnquiryDto> getEnquiriesByCustomer(Long customerId, int page, int size);
    
    Page<EnquiryDto> getEnquiriesByShop(Long shopId, int page, int size);
    
    Page<EnquiryDto> getEnquiriesByVehicle(Long vehicleId, int page, int size);
    
    Page<EnquiryDto> getEnquiriesByStatus(EnquiryStatus status, int page, int size);
    
    List<EnquiryDto> getPendingEnquiriesByShop(Long shopId);
    
    EnquiryDto updateEnquiry(Long id, EnquiryDto enquiryDto);
    
    EnquiryDto updateEnquiryStatus(Long id, EnquiryStatus status);
    
    void deleteEnquiry(Long id);
    
    EnquiryDto markAsContacted(Long id);
    
    EnquiryDto markAsClosed(Long id);
    
    EnquiryDto markAsResolved(Long id);
    
    List<EnquiryDto> getEnquiriesByStatus(EnquiryStatus status);
    
    Page<EnquiryDto> searchEnquiries(String keyword, int page, int size);
    
    List<EnquiryMessageDto> getEnquiryMessages(Long enquiryId);
    
    EnquiryMessageDto addEnquiryMessage(Long enquiryId, EnquiryMessageDto messageDto);
}
