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
import swari.sewa.module.auth.entity.RefreshToken;
import swari.sewa.module.auth.repository.RefreshTokenRepository;
import swari.sewa.module.auth.service.AuthService;
import swari.sewa.module.user.dto.UserDto;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.user.service.UserService;
import swari.sewa.module.user.repository.ShopOwnerRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
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

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    /**
     * Internal representation of an authenticated account, regardless of
     * which identity table it came from. This keeps the dual-table lookup
     * in ONE place until User/ShopOwner identities are merged into a single
     * users table (planned for the user-module migration step).
     */
    private record Account(Long id, String email, String firstName, String lastName,
                           String role, boolean active, String passwordHash) {
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
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!account.active()) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        String accessToken = jwtUtil.generateToken(account.email(), account.role());
        String refreshToken = issueRefreshToken(account.email());

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
        Account account = findAccountByEmail(stored.getUserEmail())
                .filter(Account::active)
                .orElseThrow(() -> new TokenRefreshException("Account is not active"));

        // Rotation: revoke the used token and issue a new one.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String accessToken = jwtUtil.generateToken(account.email(), account.role());
        String newRefreshToken = issueRefreshToken(account.email());

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
        return userRepository.existsByEmail(email) || shopOwnerRepository.findByEmail(email).isPresent();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Optional<Account> findAccountByEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return Optional.of(new Account(user.getId(), user.getEmail(), user.getFirstName(),
                    user.getLastName(), user.getRole().name(), user.getActive(), user.getPassword()));
        }
        return shopOwnerRepository.findByEmail(email)
                .map(owner -> new Account(owner.getId(), owner.getEmail(), owner.getFirstName(),
                        owner.getLastName(), owner.getRole().name(), owner.isActive(), owner.getPassword()));
    }

    private String issueRefreshToken(String email) {
        String rawToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .userEmail(email)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private LoginResponse buildLoginResponse(Account account, String accessToken, String refreshToken) {
        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(account.id())
                .email(account.email())
                .firstName(account.firstName())
                .lastName(account.lastName())
                .role(account.role())
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
