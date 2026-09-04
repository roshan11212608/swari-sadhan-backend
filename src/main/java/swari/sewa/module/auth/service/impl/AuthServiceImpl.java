package swari.sewa.module.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.common.dto.LoginRequest;
import swari.sewa.common.dto.LoginResponse;
import swari.sewa.common.dto.SignupRequest;
import swari.sewa.common.enums.UserRole;
import swari.sewa.common.exception.InvalidCredentialsException;
import swari.sewa.common.exception.TokenRefreshException;
import swari.sewa.common.util.JwtUtil;
import swari.sewa.module.auth.dto.MobileLoginRequest;
import swari.sewa.module.auth.entity.RefreshToken;
import swari.sewa.module.auth.repository.RefreshTokenRepository;
import swari.sewa.module.auth.service.AuthService;
import swari.sewa.module.user.dto.UserDto;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.user.service.UserService;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.shop.repository.ShopRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ShopRepository shopRepository;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    /**
     * Internal representation of an authenticated account, regardless of
     * which identity table it came from. This keeps the dual-table lookup
     * in ONE place until User/ShopOwner identities are merged into a single
     * users table (planned for the user-module migration step).
     *
     * <p>The {@code shopOwnerId} field carries the ShopOwner ID (when the
     * account is a shop owner) so that the shop-ID lookup in
     * {@link #buildLoginResponse} does not need to re-query the
     * {@code shop_owners} table by email — the email was already used to
     * load the ShopOwner in {@link #findAccountByEmail}.
     */
    private record Account(Long id, String email, String firstName, String lastName,
                           String role, boolean active, String passwordHash, String phone,
                           String customerCode, boolean mustChangePassword,
                           String approvalStatus, Long shopOwnerId) {
    }

    @Override
    public UserDto signup(SignupRequest signupRequest) {
        // Security: signup always creates PUBLIC (customer) users.
        // Role is enforced by backend, not from frontend.
        SignupRequest enforcedRequest = SignupRequest.builder()
                .email(signupRequest.getEmail())
                .password(signupRequest.getPassword())
                .firstName(signupRequest.getFirstName())
                .lastName(signupRequest.getLastName())
                .phoneNumber(signupRequest.getPhoneNumber())
                .role(UserRole.PUBLIC)
                .build();
        return userService.createUser(enforcedRequest);
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        Account account = findAccountByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), account.passwordHash())) {
            // If the email exists in the users table but the password doesn't match,
            // it might be a shop owner whose email also exists as a public user.
            // Try the shop_owners table directly before failing.
            Optional<Account> shopOwnerAccount = shopOwnerRepository.findByEmail(loginRequest.getEmail())
                    .map(owner -> new Account(owner.getId(), owner.getEmail(), owner.getFirstName(),
                            owner.getLastName(), owner.getRole().name(), owner.isActive(), owner.getPassword(),
                            owner.getPhone(), null,
                            owner.getPasswordChanged() == null || !owner.getPasswordChanged(),
                            owner.getApprovalStatus(), owner.getId()));
            if (shopOwnerAccount.isPresent()
                    && passwordEncoder.matches(loginRequest.getPassword(), shopOwnerAccount.get().passwordHash())) {
                account = shopOwnerAccount.get();
            } else {
                throw new InvalidCredentialsException("Invalid email or password");
            }
        }

        // Block login for pending/rejected shop owners
        if ("PENDING".equals(account.approvalStatus())) {
            throw new InvalidCredentialsException("Your registration is pending admin approval.");
        }
        if ("REJECTED".equals(account.approvalStatus())) {
            throw new InvalidCredentialsException("Your registration was rejected. Please contact support.");
        }

        if (!account.active()) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        String subject = accountSubject(account);
        String accessToken = jwtUtil.generateToken(subject, account.role());
        String refreshToken = issueRefreshToken(subject);

        return buildLoginResponse(account, accessToken, refreshToken);
    }

    @Override
    public LoginResponse loginWithMobile(MobileLoginRequest request) {
        String normalized = normalizeMobileForLogin(request.getMobileNumber());
        Account account = findAccountByMobile(normalized)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid mobile number or password"));

        if (!passwordEncoder.matches(request.getPassword(), account.passwordHash())) {
            throw new InvalidCredentialsException("Invalid mobile number or password");
        }

        if (!account.active()) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        String subject = accountSubject(account);
        String accessToken = jwtUtil.generateToken(subject, account.role());
        String refreshToken = issueRefreshToken(subject);

        return buildLoginResponse(account, accessToken, refreshToken);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .orElseThrow(() -> new TokenRefreshException("Invalid refresh token"));

        if (Boolean.TRUE.equals(stored.getRevoked()) || stored.isExpired()) {
            throw new TokenRefreshException("Refresh token is expired or revoked");
        }

        // Re-load the account so deactivated users cannot refresh their session.
        // The stored user identifier may be an email (email users / shop owners)
        // or a mobile number (mobile-only public users).
        Account account = findAccountByIdentifier(stored.getUserEmail())
                .filter(Account::active)
                .orElseThrow(() -> new TokenRefreshException("Account is not active"));

        // Rotation: revoke the used token and issue a new one.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String subject = accountSubject(account);
        String accessToken = jwtUtil.generateToken(subject, account.role());
        String newRefreshToken = issueRefreshToken(subject);

        return buildLoginResponse(account, accessToken, newRefreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .ifPresent(stored -> {
                    stored.setRevoked(true);
                    refreshTokenRepository.save(stored);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        // A shop owner's application state is the source of truth. A REJECTED
        // application is allowed to re-apply, so the email is considered
        // available even if a mirrored users row exists from a previous
        // approval cycle. PENDING and APPROVED remain unavailable.
        //
        // Optimization: use a lightweight projection that fetches only the
        // approval_status column instead of loading the entire ShopOwner
        // entity (40+ columns including password hash, documents, etc.).
        Optional<String> approvalStatusOpt = shopOwnerRepository.findApprovalStatusByEmail(email);
        if (approvalStatusOpt.isPresent()) {
            String status = approvalStatusOpt.get();
            if ("REJECTED".equals(status)) {
                return false;
            }
            return true; // PENDING or APPROVED
        }
        // No shop owner record — fall back to the users table for public users
        // and other roles. existsByEmail is a COUNT query, not a full entity load.
        return userRepository.existsByEmail(email);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Optional<Account> findAccountByEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Shop owners are mirrored into the users table by other flows, but
            // shop_owners stays the source of truth for approval state and for
            // whether the temporary password has been replaced. Reading those
            // from the mirror would report "nothing to do" and let an owner
            // skip the forced password change (and skip approval gating).
            Long shopOwnerId = null;
            boolean mustChangePassword = false;
            String approvalStatus = null;
            if (user.getRole() == UserRole.SHOP_OWNER) {
                Optional<ShopOwner> ownerRow = shopOwnerRepository.findByEmail(email);
                if (ownerRow.isPresent()) {
                    ShopOwner owner = ownerRow.get();
                    shopOwnerId = owner.getId();
                    mustChangePassword = owner.getPasswordChanged() == null || !owner.getPasswordChanged();
                    approvalStatus = owner.getApprovalStatus();
                }
            }
            return Optional.of(new Account(user.getId(), user.getEmail(), user.getFirstName(),
                    user.getLastName(), user.getRole().name(), user.getActive(), user.getPassword(),
                    user.getPhoneNumber(), user.getCustomerCode(), mustChangePassword, approvalStatus,
                    shopOwnerId));
        }
        return shopOwnerRepository.findByEmail(email)
                .map(owner -> new Account(owner.getId(), owner.getEmail(), owner.getFirstName(),
                        owner.getLastName(), owner.getRole().name(), owner.isActive(), owner.getPassword(),
                        owner.getPhone(), null,
                        owner.getPasswordChanged() == null || !owner.getPasswordChanged(),
                        owner.getApprovalStatus(), owner.getId()));
    }

    private Optional<Account> findAccountByMobile(String mobile) {
        return userRepository.findByPhoneNumber(mobile)
                .map(user -> {
                    Long shopOwnerId = null;
                    boolean mustChangePassword = false;
                    String approvalStatus = null;
                    if (user.getRole() == UserRole.SHOP_OWNER && user.getEmail() != null) {
                        Optional<ShopOwner> ownerRow = shopOwnerRepository.findByEmail(user.getEmail());
                        if (ownerRow.isPresent()) {
                            ShopOwner owner = ownerRow.get();
                            shopOwnerId = owner.getId();
                            mustChangePassword = owner.getPasswordChanged() == null || !owner.getPasswordChanged();
                            approvalStatus = owner.getApprovalStatus();
                        }
                    }
                    return new Account(user.getId(), user.getEmail(), user.getFirstName(),
                            user.getLastName(), user.getRole().name(), user.getActive(),
                            user.getPassword(), user.getPhoneNumber(), user.getCustomerCode(),
                            mustChangePassword, approvalStatus, shopOwnerId);
                });
    }

    /**
     * Look up an account by an identifier that may be either an email or a
     * mobile number. Used by the refresh-token flow, where the stored
     * identifier depends on how the user logged in.
     */
    private Optional<Account> findAccountByIdentifier(String identifier) {
        Optional<Account> byEmail = findAccountByEmail(identifier);
        if (byEmail.isPresent()) return byEmail;
        return findAccountByMobile(identifier);
    }

    /**
     * Returns the JWT/refresh-token subject for an account: the email when
     * present, otherwise the phone number (mobile-only public users).
     */
    private String accountSubject(Account account) {
        return account.email() != null ? account.email() : account.phone();
    }

    static String normalizeMobileForLogin(String raw) {
        if (raw == null) throw new InvalidCredentialsException("Mobile number is required");
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("977")) digits = digits.substring(3);
        if (digits.length() == 10 && (digits.startsWith("98")
                || digits.startsWith("97") || digits.startsWith("96"))) {
            return "+977" + digits;
        }
        throw new InvalidCredentialsException("Invalid mobile number or password");
    }

    private String issueRefreshToken(String email) {
        String rawToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .userEmail(email)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private LoginResponse buildLoginResponse(Account account, String accessToken, String refreshToken) {
        Long shopId = null;
        // If the account is a shop owner, fetch their shop ID using the
        // shopOwnerId already loaded during findAccountByEmail — no need to
        // re-query shop_owners by email. Uses a lightweight ID-only projection
        // instead of loading full Shop entities.
        if ("SHOP_OWNER".equals(account.role()) && account.shopOwnerId() != null) {
            List<Long> shopIds = shopRepository.findShopIdsByShopOwnerId(account.shopOwnerId());
            shopId = shopIds.isEmpty() ? null : shopIds.get(0);
        }

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(account.id())
                .email(account.email())
                .firstName(account.firstName())
                .lastName(account.lastName())
                .role(account.role())
                .shopId(shopId)
                .phone(account.phone())
                .customerCode(account.customerCode())
                .mustChangePassword(account.mustChangePassword())
                .build();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
