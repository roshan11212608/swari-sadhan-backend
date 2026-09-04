package swari.sewa.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import swari.sewa.common.util.JwtUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Stateless JWT authentication filter.
 *
 * <p>On every request that carries a {@code Bearer} token, this filter:
 * <ol>
 *   <li>Parses and validates the JWT <b>once</b> (signature + expiration).</li>
 *   <li>Extracts the subject (email or mobile) and the {@code role} claim
 *       directly from the token.</li>
 *   <li>Builds a {@link UsernamePasswordAuthenticationToken} with the
 *       authorities derived from the role claim — <b>without any database
 *       lookup</b>.</li>
 * </ol>
 *
 * <p>This is the standard stateless JWT pattern. The JWT's signature proves
 * authenticity, and the role claim is trusted because it was set by the
 * backend at login time and cannot be tampered with without the signing key.
 *
 * <p>The previous implementation called
 * {@code userDetailsService.loadUserByUsername(subject)} on every request,
 * which triggered 1–3 database queries per authenticated API call. On a
 * hosted environment with a potentially sleeping TiDB Cloud free-tier
 * database, this added significant latency to every request.
 *
 * <p>Trade-off: if a user is deactivated after their JWT is issued, they can
 * still access the API until the token expires (default 24h). This is an
 * accepted trade-off of stateless JWT. The refresh-token flow already checks
 * account active status, so a deactivated user cannot obtain a new access
 * token after the current one expires.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            final String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            final String jwt = authHeader.substring(7);

            // Single parse: validates signature + expiration and returns claims.
            Optional<Claims> claimsOpt = jwtUtil.parseClaims(jwt);
            if (claimsOpt.isEmpty()) {
                log.debug("JWT Filter - Invalid or expired token for path: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = claimsOpt.get();
            final String subject = claims.getSubject();
            final String role = claims.get("role", String.class);

            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Build authorities from the role claim — matches the format
                // produced by UserDetailsServiceImpl: both ROLE_<role> and
                // the bare role name, so hasRole() and hasAuthority() both work.
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role),
                        new SimpleGrantedAuthority(role)
                );

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(subject, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWT Filter - Authentication set for user: {}", subject);
            }

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("JWT Filter - Unexpected error on path: {} - {}", request.getRequestURI(), ex.getMessage(), ex);
            filterChain.doFilter(request, response);
        }
    }
}
