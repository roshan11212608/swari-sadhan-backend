package swari.sewa.module.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import swari.sewa.common.util.JwtUtil;
import swari.sewa.module.auth.repository.RefreshTokenRepository;
import swari.sewa.module.auth.service.impl.AuthServiceImpl;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.user.service.UserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link AuthServiceImpl#emailExists(String)} method,
 * verifying that it uses lightweight existence/projection queries instead
 * of loading full entities.
 */
class AuthServiceEmailExistsTest {

    private AuthServiceImpl authService;
    private ShopOwnerRepository shopOwnerRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        shopOwnerRepository = mock(ShopOwnerRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        UserService userService = mock(UserService.class);
        ShopRepository shopRepository = mock(ShopRepository.class);
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "swariSewaSecretKeyForJWTTokenGenerationAndValidation2024");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 86400000L);
        ReflectionTestUtils.invokeMethod(jwtUtil, "initSigningKey");

        authService = new AuthServiceImpl(
                userRepository, shopOwnerRepository, refreshTokenRepository,
                userService, new BCryptPasswordEncoder(), jwtUtil, shopRepository);
    }

    @Test
    void emailExists_returnsTrueForApprovedShopOwner() {
        when(shopOwnerRepository.findApprovalStatusByEmail("owner@shop.com"))
                .thenReturn(Optional.of("APPROVED"));

        assertTrue(authService.emailExists("owner@shop.com"));
        // Should NOT fall through to userRepository since shop owner was found
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void emailExists_returnsFalseForRejectedShopOwner() {
        when(shopOwnerRepository.findApprovalStatusByEmail("rejected@shop.com"))
                .thenReturn(Optional.of("REJECTED"));

        assertFalse(authService.emailExists("rejected@shop.com"));
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void emailExists_returnsTrueForPendingShopOwner() {
        when(shopOwnerRepository.findApprovalStatusByEmail("pending@shop.com"))
                .thenReturn(Optional.of("PENDING"));

        assertTrue(authService.emailExists("pending@shop.com"));
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void emailExists_fallsBackToUsersTableWhenNoShopOwner() {
        when(shopOwnerRepository.findApprovalStatusByEmail("user@test.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("user@test.com"))
                .thenReturn(true);

        assertTrue(authService.emailExists("user@test.com"));
        verify(userRepository).existsByEmail("user@test.com");
    }

    @Test
    void emailExists_returnsFalseWhenNotInEitherTable() {
        when(shopOwnerRepository.findApprovalStatusByEmail("nobody@test.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("nobody@test.com"))
                .thenReturn(false);

        assertFalse(authService.emailExists("nobody@test.com"));
    }

    @Test
    void emailExists_usesProjectionNotFullEntityLoad() {
        // This test documents the optimization: findApprovalStatusByEmail
        // (a single-column projection) is called instead of findByEmail
        // (which would load the full 40+ column entity).
        when(shopOwnerRepository.findApprovalStatusByEmail("owner@shop.com"))
                .thenReturn(Optional.of("APPROVED"));

        authService.emailExists("owner@shop.com");

        // Verify the projection method is called, NOT the full entity load
        verify(shopOwnerRepository).findApprovalStatusByEmail("owner@shop.com");
        verify(shopOwnerRepository, never()).findByEmail(any());
    }
}
