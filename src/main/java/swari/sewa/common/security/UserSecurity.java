package swari.sewa.common.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;

@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {

    private static final Logger log = LoggerFactory.getLogger(UserSecurity.class);
    private final UserRepository userRepository;

    /**
     * Check if the authenticated user (by email) owns the resource with the given user ID.
     * 
     * @param userId The user ID to check ownership against
     * @param email The authenticated user's email (from authentication.getName())
     * @return true if the authenticated user owns the resource, false otherwise
     */
    public boolean isOwner(Long userId, String email) {
        log.info("UserSecurity.isOwner called - userId: {}, email: {}", userId, email);
        
        if (userId == null || email == null) {
            log.warn("UserSecurity.isOwner - userId or email is null");
            return false;
        }

        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("UserSecurity.isOwner - User not found with id: {}", userId);
                return false;
            }

            log.info("UserSecurity.isOwner - Found user with email: {}", user.getEmail());
            
            // Compare the user's email with the authenticated email
            boolean result = email.equals(user.getEmail());
            log.info("UserSecurity.isOwner - Comparison result: {} (authenticated: {}, user: {})", result, email, user.getEmail());
            return result;
        } catch (Exception e) {
            log.error("UserSecurity.isOwner - Exception: ", e);
            return false;
        }
    }
}
