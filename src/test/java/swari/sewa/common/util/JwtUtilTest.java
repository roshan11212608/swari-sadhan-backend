package swari.sewa.common.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtUtil}.
 *
 * <p>These tests verify the JWT generation/parsing contract that the
 * optimized {@code JwtAuthenticationFilter} relies on: a single call to
 * {@link JwtUtil#parseClaims(String)} must validate the signature,
 * enforce expiration, and return both the subject and the role claim.
 *
 * <p>No Spring context is required — the secret and expiration are injected
 * directly via {@link ReflectionTestUtils}, and {@link JwtUtil#initSigningKey()}
 * is called manually in {@link BeforeEach}.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "swariSewaSecretKeyForJWTTokenGenerationAndValidation2024";
    private static final long EXPIRATION_MS = 86_400_000L; // 24h

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", EXPIRATION_MS);
        ReflectionTestUtils.invokeMethod(jwtUtil, "initSigningKey");
    }

    @Test
    void generateToken_andParseClaims_returnsSubjectAndRole() {
        String token = jwtUtil.generateToken("user@example.com", "SHOP_OWNER");

        Optional<Claims> claimsOpt = jwtUtil.parseClaims(token);
        assertTrue(claimsOpt.isPresent(), "Valid token should parse successfully");

        Claims claims = claimsOpt.get();
        assertEquals("user@example.com", claims.getSubject());
        assertEquals("SHOP_OWNER", claims.get("role", String.class));
    }

    @Test
    void parseClaims_returnsEmptyForMalformedToken() {
        Optional<Claims> claims = jwtUtil.parseClaims("not.a.jwt");
        assertTrue(claims.isEmpty());
    }

    @Test
    void parseClaims_returnsEmptyForTamperedToken() {
        String token = jwtUtil.generateToken("user@example.com", "PUBLIC");
        // Flip a character in the token body to break the signature
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        Optional<Claims> claims = jwtUtil.parseClaims(tampered);
        assertTrue(claims.isEmpty());
    }

    @Test
    void parseClaims_returnsEmptyForExpiredToken() {
        // Generate a token, then set expiration to the past by using a
        // negative expiration value.
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", -1000L);
        String expiredToken = jwtUtil.generateToken("user@example.com", "PUBLIC");

        Optional<Claims> claims = jwtUtil.parseClaims(expiredToken);
        assertTrue(claims.isEmpty(), "Expired token should not parse");
    }

    @Test
    void parseClaims_returnsEmptyForWrongSecret() {
        String token = jwtUtil.generateToken("user@example.com", "SUPERADMIN");

        // Create a second JwtUtil with a different secret
        JwtUtil otherUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherUtil, "jwtSecret", "aDifferentSecretKeyThatIsLongEnough1234567890");
        ReflectionTestUtils.setField(otherUtil, "jwtExpiration", EXPIRATION_MS);
        otherUtil.initSigningKey();

        Optional<Claims> claims = otherUtil.parseClaims(token);
        assertTrue(claims.isEmpty(), "Token signed with a different key should not parse");
    }

    @Test
    void validateToken_returnsTrueForValidToken() {
        String token = jwtUtil.generateToken("user@example.com", "SHOP_OWNER");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_returnsFalseForInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid"));
    }

    @Test
    void getEmailFromToken_returnsSubject() {
        String token = jwtUtil.generateToken("user@example.com", "SHOP_OWNER");
        assertEquals("user@example.com", jwtUtil.getEmailFromToken(token));
    }

    @Test
    void getRoleFromToken_returnsRoleClaim() {
        String token = jwtUtil.generateToken("user@example.com", "SUPERADMIN");
        assertEquals("SUPERADMIN", jwtUtil.getRoleFromToken(token));
    }

    @Test
    void isTokenExpired_returnsFalseForValidToken() {
        String token = jwtUtil.generateToken("user@example.com", "PUBLIC");
        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void isTokenExpired_returnsTrueForExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", -1000L);
        String expiredToken = jwtUtil.generateToken("user@example.com", "PUBLIC");
        assertTrue(jwtUtil.isTokenExpired(expiredToken));
    }
}
