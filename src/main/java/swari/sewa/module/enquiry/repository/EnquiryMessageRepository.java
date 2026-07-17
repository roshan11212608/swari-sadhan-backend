package swari.sewa.module.enquiry.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import swari.sewa.module.enquiry.entity.EnquiryMessage;

@Repository
public interface EnquiryMessageRepository extends JpaRepository<EnquiryMessage, Long> {
    List<EnquiryMessage> findByEnquiryIdOrderByCreatedAtAsc(Long enquiryId);
}
