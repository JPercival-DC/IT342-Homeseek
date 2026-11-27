package com.homeseek.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeseek.dto.ErrorResponse;
import com.homeseek.repository.RevokedTokenRepository;
import com.homeseek.repository.UserRepository;
import com.homeseek.security.JwtAuthenticationFilter;
import com.homeseek.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless JWT security. Server-side role rules (FR-006): no token gives 401, a token of the wrong role gives 403.
 * CSRF protection is off because authentication uses an Authorization header, not cookies.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, UserRepository users,
            RevokedTokenRepository revokedTokens, ObjectMapper mapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login",
                                "/api/password/forgot", "/api/password/reset").permitAll()
                        .requestMatchers("/api/tenant/**").hasRole("TENANT")
                        .requestMatchers("/api/owner/**").hasRole("OWNER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> writeError(res, mapper, 401, "Authentication required"))
                        .accessDeniedHandler((req, res, e) ->
                                writeError(res, mapper, 403, "You do not have permission to access this resource")))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, users, revokedTokens),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** BCrypt with cost factor 12 (SRS requires 10 or higher). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(props.cors().allowedOrigin()));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    private static void writeError(HttpServletResponse res, ObjectMapper mapper, int status, String message)
            throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getWriter(), ErrorResponse.of(message));
    }
}
