package swari.sewa.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // Cached signing key — avoids re-deriving the HMAC key on every token
    // generation/validation. The secret is immutable for the lifetime of the
    // application, so the key only needs to be computed once.
    private SecretKey signingKey;

    @PostConstruct
    void initSigningKey() {
        signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).map(Claims::getSubject).orElse(null);
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).map(c -> c.get("role", String.class)).orElse(null);
    }

    public boolean validateToken(String token) {
        return parseClaims(token).isPresent();
    }

    public boolean isTokenExpired(String token) {
        Optional<Claims> claims = parseClaims(token);
        return claims.map(c -> c.getExpiration().before(new Date())).orElse(true);
    }

    /**
     * Parse and validate a JWT in a single operation. This verifies the
     * signature and expiration, returning the claims only when the token is
     * valid. Callers that need both the subject and the role (e.g. the JWT
     * filter) should use this instead of calling {@link #getEmailFromToken}
     * and {@link #validateToken} separately, which would parse the token twice.
     *
     * @return the validated claims, or {@code Optional.empty()} if the token
     *         is malformed, expired, or has an invalid signature
     */
    public Optional<Claims> parseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
