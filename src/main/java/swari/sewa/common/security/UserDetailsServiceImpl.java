package swari.sewa.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final ShopOwnerRepository shopOwnerRepository;

    @Override
    public UserDetails loadUserByUsername(String subject) throws UsernameNotFoundException {
        // Subject can be an email (email-based users / shop owners) or a
        // mobile number (mobile-only public users). Try email first, then
        // phone, then shop owners.
        User user = userRepository.findByEmail(subject).orElse(null);
        if (user == null) {
            user = userRepository.findByPhoneNumber(subject).orElse(null);
        }
        if (user != null) {
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail() != null ? user.getEmail() : user.getPhoneNumber())
                    .password(user.getPassword())
                    .authorities("ROLE_" + user.getRole().name(), user.getRole().name())
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(!user.getIsActive())
                    .build();
        }

        ShopOwner shopOwner = shopOwnerRepository.findByEmail(subject)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + subject));

        return org.springframework.security.core.userdetails.User.builder()
                .username(shopOwner.getEmail())
                .password(shopOwner.getPassword())
                .authorities("ROLE_" + shopOwner.getRole().name(), shopOwner.getRole().name())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!shopOwner.getActive())
                .build();
    }
}
