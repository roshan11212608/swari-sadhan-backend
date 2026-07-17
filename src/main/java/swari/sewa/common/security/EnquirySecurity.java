package swari.sewa.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import swari.sewa.module.enquiry.entity.Enquiry;
import swari.sewa.module.enquiry.repository.EnquiryRepository;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;

@Component("enquirySecurity")
@RequiredArgsConstructor
public class EnquirySecurity {

    private final EnquiryRepository enquiryRepository;
    private final UserRepository userRepository;

    /**
     * Check if the authenticated customer (by email) owns the enquiry with the given enquiry ID.
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
            Enquiry enquiry = enquiryRepository.findById(enquiryId).orElse(null);
            if (enquiry == null || enquiry.getCustomer() == null) {
                return false;
            }

            // Compare the customer's email with the authenticated email
            return email.equals(enquiry.getCustomer().getEmail());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the authenticated shop owner (by email) owns the enquiry with the given enquiry ID.
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
            Enquiry enquiry = enquiryRepository.findById(enquiryId).orElse(null);
            if (enquiry == null || enquiry.getShop() == null || enquiry.getShop().getShopOwner() == null) {
                return false;
            }

            // Compare the shop owner's email with the authenticated email
            return email.equals(enquiry.getShop().getShopOwner().getEmail());
        } catch (Exception e) {
            return false;
        }
    }
}
