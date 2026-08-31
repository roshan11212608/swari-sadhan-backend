package swari.sewa.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import swari.sewa.common.util.JwtUtil;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            final String authHeader = request.getHeader("Authorization");
            final String jwt;
            final String userEmail;

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("JWT Filter - No Authorization header or not Bearer for path: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            jwt = authHeader.substring(7);

            try {
                userEmail = jwtUtil.getEmailFromToken(jwt);
                log.debug("JWT Filter - Extracted email from token: {}", userEmail);
            } catch (Exception ex) {
                // Invalid token (malformed/expired/signature). Don't block the request —
                // continue the filter chain as unauthenticated so permitAll endpoints still work.
                log.warn("JWT Filter - Failed to extract email from token: {}", ex.getMessage());
                filterChain.doFilter(request, response);
                return;
            }

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails;
                try {
                    userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                } catch (Exception ex) {
                    log.warn("JWT Filter - User not found or error loading user for token: {}", userEmail);
                    filterChain.doFilter(request, response);
                    return;
                }

                boolean valid;
                try {
                    valid = jwtUtil.validateToken(jwt);
                } catch (Exception ex) {
                    log.warn("JWT Filter - Token validation failed: {}", ex.getMessage());
                    filterChain.doFilter(request, response);
                    return;
                }

                log.debug("JWT Filter - Token valid: {}, authorities: {}", valid, userDetails.getAuthorities());

                if (valid) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("JWT Filter - Authentication set for user: {}", userEmail);
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("JWT Filter - Unexpected error on path: {} - {}", request.getRequestURI(), ex.getMessage(), ex);
            filterChain.doFilter(request, response);
        }
    }
}
