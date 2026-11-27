package com.homeseek.security;

import com.homeseek.repository.RevokedTokenRepository;
import com.homeseek.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a request from its "Authorization: Bearer" token. The token must be validly signed, not expired,
 * not revoked by logout, issued under the user's current token version, and its user must still exist and be active. The role always comes from the database.
 * Not a Spring bean on purpose: it is added to the security chain only (see SecurityConfig).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;
    private final RevokedTokenRepository revokedTokens;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users, RevokedTokenRepository revokedTokens) {
        this.jwtService = jwtService;
        this.users = users;
        this.revokedTokens = revokedTokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.parse(header.substring(7).trim()).ifPresent(claims -> {
                if (revokedTokens.existsById(claims.jti())) {
                    return;
                }
                users.findById(claims.userId()).filter(u -> u.isActive() && u.getTokenVersion() == claims.version()).ifPresent(u -> {
                    var principal = new AuthUser(u.getId(), u.getEmail(), u.getRole(), claims.jti(), claims.expiresAt());
                    var auth = new UsernamePasswordAuthenticationToken(principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            });
        }
        chain.doFilter(request, response);
    }
}
