package swari.sewa.module.enquiry.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminEnquiryService {
    
    Page<Object> getAllEnquiries(Pageable pageable, String status, String search);
    
    Object getEnquiryById(Long id);
    
    void respondToEnquiry(Long id, String response);
    
    void markEnquiryAsResponded(Long id);
    
    void closeEnquiry(Long id);
    
    void deleteEnquiry(Long id);
    
    Page<Object> getPendingEnquiries(Pageable pageable);
    
    Object getEnquiryStats();
}
