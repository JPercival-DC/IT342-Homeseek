package com.homeseek.security;

import com.homeseek.config.AppProperties;
import com.homeseek.model.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues and verifies HS256-signed JWTs (FR-004: 24-hour expiry). */
@Service
public class JwtService {

    public record IssuedToken(String token, String jti, Instant expiresAt) {}

    public record TokenClaims(Long userId, String jti, Instant expiresAt, int version) {}

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(AppProperties props) {
        String secret = props.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofHours(props.jwt().expirationHours());
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti)
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())   // informational only; authorization uses the database role
                .claim("ver", user.getTokenVersion())   // must match the database, so a password change ends this token
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, jti, expiresAt);
    }

    /** Returns the claims if the signature and expiry are valid, otherwise empty. */
    public Optional<TokenClaims> parse(String token) {
        try {
            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Integer version = claims.get("ver", Integer.class);       // tokens from before Increment 2 have none: version 0
            return Optional.of(new TokenClaims(Long.valueOf(claims.getSubject()), claims.getId(),
                    claims.getExpiration().toInstant(), version == null ? 0 : version));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
