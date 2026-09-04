package swari.sewa.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import swari.sewa.module.enquiry.repository.EnquiryRepository;

@Component("enquirySecurity")
@RequiredArgsConstructor
public class EnquirySecurity {

    private final EnquiryRepository enquiryRepository;

    /**
     * Check if the authenticated customer (by email) owns the enquiry with the given enquiry ID.
     * Uses a lightweight projection query to avoid loading the full enquiry entity and
     * its lazy customer relationship.
     *
     * @param enquiryId The enquiry ID to check ownership against
     * @param email The authenticated user's email (from authentication.getName())
     * @return true if the authenticated customer owns the enquiry, false otherwise
     */
    public boolean isCustomer(Long enquiryId, String email) {
        if (enquiryId == null || email == null) {
            return false;
        }

        try {
            return enquiryRepository.findCustomerEmailById(enquiryId)
                    .map(email::equals)
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the authenticated shop owner (by email) owns the enquiry with the given enquiry ID.
     * Uses a lightweight projection query to avoid loading the full enquiry entity and
     * its lazy shop and shopOwner relationships.
     *
     * @param enquiryId The enquiry ID to check ownership against
     * @param email The authenticated user's email (from authentication.getName())
     * @return true if the authenticated shop owner owns the enquiry, false otherwise
     */
    public boolean isShopOwner(Long enquiryId, String email) {
        if (enquiryId == null || email == null) {
            return false;
        }

        try {
            return enquiryRepository.findShopOwnerEmailById(enquiryId)
                    .map(email::equals)
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }
}
