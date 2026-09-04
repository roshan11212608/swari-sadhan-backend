package swari.sewa.module.enquiry.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.enquiry.entity.Enquiry;
import swari.sewa.module.enquiry.repository.EnquiryRepository;
import swari.sewa.common.enums.EnquiryStatus;
import swari.sewa.module.enquiry.service.AdminEnquiryService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminEnquiryServiceImpl implements AdminEnquiryService {

    private final EnquiryRepository enquiryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getAllEnquiries(Pageable pageable, String status, String search) {
        Page<Enquiry> enquiries;

        if (search != null && !search.trim().isEmpty()) {
            enquiries = enquiryRepository.searchByCustomerNameWithCustomerVehicleShop(
                    search, search, pageable);
        } else if (status != null && !status.trim().isEmpty()) {
            EnquiryStatus enquiryStatus = EnquiryStatus.valueOf(status.toUpperCase());
            enquiries = enquiryRepository.findByStatusWithCustomerVehicleShop(enquiryStatus, pageable);
        } else {
            enquiries = enquiryRepository.findAllWithCustomerVehicleShop(pageable);
        }
        
        return enquiries.map(enquiry -> {
            Map<String, Object> enquiryData = new HashMap<>();
            enquiryData.put("id", enquiry.getId());
            enquiryData.put("customerName", enquiry.getCustomer().getFirstName() + " " + enquiry.getCustomer().getLastName());
            enquiryData.put("customerEmail", enquiry.getCustomer().getEmail());
            enquiryData.put("customerPhone", enquiry.getCustomer().getPhone());
            enquiryData.put("vehicleTitle", enquiry.getVehicle().getTitle());
            enquiryData.put("shopName", enquiry.getShop().getName());
            enquiryData.put("status", enquiry.getStatus());
            enquiryData.put("message", enquiry.getMessage());
            enquiryData.put("response", enquiry.getResponse());
            enquiryData.put("createdAt", enquiry.getCreatedAt());
            enquiryData.put("respondedAt", enquiry.getRespondedAt());
            return enquiryData;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Object getEnquiryById(Long id) {
        Enquiry enquiry = enquiryRepository.findByIdWithCustomerVehicleShop(id)
                .orElseThrow(() -> new RuntimeException("Enquiry not found"));
        
        Map<String, Object> enquiryData = new HashMap<>();
        enquiryData.put("id", enquiry.getId());
        enquiryData.put("customerName", enquiry.getCustomer().getFirstName() + " " + enquiry.getCustomer().getLastName());
        enquiryData.put("customerEmail", enquiry.getCustomer().getEmail());
        enquiryData.put("customerPhone", enquiry.getCustomer().getPhone());
        enquiryData.put("vehicleTitle", enquiry.getVehicle().getTitle());
        enquiryData.put("vehicleId", enquiry.getVehicle().getId());
        enquiryData.put("shopName", enquiry.getShop().getName());
        enquiryData.put("shopId", enquiry.getShop().getId());
        enquiryData.put("status", enquiry.getStatus());
        enquiryData.put("message", enquiry.getMessage());
        enquiryData.put("response", enquiry.getResponse());
        enquiryData.put("createdAt", enquiry.getCreatedAt());
        enquiryData.put("respondedAt", enquiry.getRespondedAt());
        enquiryData.put("updatedAt", enquiry.getUpdatedAt());
        
        return enquiryData;
    }

    @Override
    public void respondToEnquiry(Long id, String response) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enquiry not found"));
        
        enquiry.setResponse(response);
        enquiry.setStatus(EnquiryStatus.RESPONDED);
        enquiry.setRespondedAt(LocalDateTime.now());
        enquiryRepository.save(enquiry);
    }

    @Override
    public void markEnquiryAsResponded(Long id) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enquiry not found"));
        
        enquiry.setStatus(EnquiryStatus.RESPONDED);
        enquiry.setRespondedAt(LocalDateTime.now());
        enquiryRepository.save(enquiry);
    }

    @Override
    public void closeEnquiry(Long id) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enquiry not found"));
        
        enquiry.setStatus(EnquiryStatus.CLOSED);
        enquiryRepository.save(enquiry);
    }

    @Override
    public void deleteEnquiry(Long id) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enquiry not found"));
        
        enquiryRepository.delete(enquiry);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Object> getPendingEnquiries(Pageable pageable) {
        return enquiryRepository.findByStatusWithCustomerVehicleShop(EnquiryStatus.PENDING, pageable)
                .map(enquiry -> {
                    Map<String, Object> enquiryData = new HashMap<>();
                    enquiryData.put("id", enquiry.getId());
                    enquiryData.put("customerName", enquiry.getCustomer().getFirstName() + " " + enquiry.getCustomer().getLastName());
                    enquiryData.put("customerEmail", enquiry.getCustomer().getEmail());
                    enquiryData.put("customerPhone", enquiry.getCustomer().getPhone());
                    enquiryData.put("vehicleTitle", enquiry.getVehicle().getTitle());
                    enquiryData.put("shopName", enquiry.getShop().getName());
                    enquiryData.put("message", enquiry.getMessage());
                    enquiryData.put("createdAt", enquiry.getCreatedAt());
                    return enquiryData;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Object getEnquiryStats() {
        long totalEnquiries = enquiryRepository.count();
        long pendingEnquiries = enquiryRepository.countByStatus(EnquiryStatus.PENDING);
        long respondedEnquiries = enquiryRepository.countByStatus(EnquiryStatus.RESPONDED);
        long closedEnquiries = enquiryRepository.countByStatus(EnquiryStatus.CLOSED);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalEnquiries);
        stats.put("pending", pendingEnquiries);
        stats.put("responded", respondedEnquiries);
        stats.put("closed", closedEnquiries);
        stats.put("responseRate", totalEnquiries > 0 ? (respondedEnquiries * 100.0 / totalEnquiries) : 0);
        
        return stats;
    }
}
