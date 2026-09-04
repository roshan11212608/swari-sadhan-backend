package swari.sewa.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import swari.sewa.common.util.JwtUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 *
 * <p>These tests verify the core optimization: the filter must build
 * authentication directly from JWT claims <b>without any database lookup</b>.
 * The filter is constructed with only {@link JwtUtil} — no
 * {@code UserDetailsService} — and must still produce a valid
 * {@link Authentication} with the correct authorities.
 */
class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;

    private static final String SECRET = "swariSewaSecretKeyForJWTTokenGenerationAnd Validation2024LongEnough";
    private static final long EXPIRATION_MS = 86_400_000L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", EXPIRATION_MS);
        // Invoke the @PostConstruct method via reflection (package-private)
        ReflectionTestUtils.invokeMethod(jwtUtil, "initSigningKey");

        filter = new JwtAuthenticationFilter(jwtUtil);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_setsAuthenticationFromValidToken() throws Exception {
        String token = jwtUtil.generateToken("owner@shop.com", "SHOP_OWNER");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, resp) -> {};

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication should be set for a valid token");
        assertEquals("owner@shop.com", auth.getName());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SHOP_OWNER")));
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SHOP_OWNER")));
    }

    @Test
    void doFilter_doesNotSetAuthenticationForMissingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, resp) -> {};

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_doesNotSetAuthenticationForInvalidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, resp) -> {};

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Invalid token should not set authentication");
    }

    @Test
    void doFilter_doesNotSetAuthenticationForExpiredToken() throws Exception {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", -1000L);
        String expiredToken = jwtUtil.generateToken("owner@shop.com", "SHOP_OWNER");
        // Reset expiration for the filter's JwtUtil (same instance)
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", EXPIRATION_MS);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, resp) -> {};

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Expired token should not set authentication");
    }

    @Test
    void doFilter_setsAuthenticationForSuperadminRole() throws Exception {
        String token = jwtUtil.generateToken("admin@swari.com", "SUPERADMIN");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, resp) -> {};

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN")));
    }

    @Test
    void doFilter_setsAuthenticationForPublicRole() throws Exception {
        String token = jwtUtil.generateToken("+9779812345678", "PUBLIC");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, resp) -> {};

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("+9779812345678", auth.getName());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PUBLIC")));
    }
}
