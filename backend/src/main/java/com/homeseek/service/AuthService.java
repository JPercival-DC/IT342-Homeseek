package com.homeseek.service;

import com.homeseek.dto.LoginRequest;
import com.homeseek.dto.LoginResponse;
import com.homeseek.dto.RegisterRequest;
import com.homeseek.dto.UserResponse;
import com.homeseek.exception.ApiException;
import com.homeseek.model.AuthProvider;
import com.homeseek.model.RevokedToken;
import com.homeseek.model.Role;
import com.homeseek.model.User;
import com.homeseek.repository.RevokedTokenRepository;
import com.homeseek.repository.UserRepository;
import com.homeseek.security.AuthUser;
import com.homeseek.security.JwtService;
import com.homeseek.security.LoginAttemptService;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration (FR-001..FR-003), login (FR-004, FR-007), logout, and current-user lookup. */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository users;
    private final RevokedTokenRepository revokedTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginAttemptService attempts;
    /** Hash of a random string, compared against when the email is unknown so response time does not reveal it. */
    private final String dummyHash;

    public AuthService(UserRepository users, RevokedTokenRepository revokedTokens, PasswordEncoder encoder,
            JwtService jwt, LoginAttemptService attempts) {
        this.users = users;
        this.revokedTokens = revokedTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.attempts = attempts;
        this.dummyHash = encoder.encode(java.util.UUID.randomUUID().toString());
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.email());
        if (users.existsByEmail(email)) {
            throw emailTaken();
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone().trim());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setRole(Role.valueOf(req.role()));       // validated as TENANT or OWNER; ADMIN can never come from here
        user.setAuthProvider(AuthProvider.LOCAL);
        try {
            return UserResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw emailTaken();                        // two registrations with the same email raced
        }
    }

    public LoginResponse login(LoginRequest req) {
        String email = normalizeEmail(req.email());
        Duration locked = attempts.remainingLock(email);
        if (!locked.isZero()) {
            long minutes = Math.max(1, (locked.getSeconds() + 59) / 60);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Try again in " + minutes + " minute(s).");
        }
        User user = users.findByEmail(email).orElse(null);
        String hash = user != null && user.getPasswordHash() != null ? user.getPasswordHash() : dummyHash;
        boolean tooLong = req.password().getBytes(StandardCharsets.UTF_8).length > 72;   // BCrypt limit
        boolean passwordOk = !tooLong && encoder.matches(req.password(), hash);
        if (user == null || user.getAuthProvider() != AuthProvider.LOCAL || !passwordOk) {
            attempts.recordFailure(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is disabled. Please contact the administrator.");
        }
        attempts.reset(email);
        JwtService.IssuedToken issued = jwt.issue(user);
        return new LoginResponse(issued.token(), issued.expiresAt(), UserResponse.from(user));
    }

    /** Ends the session: the token stops working immediately, even though its expiry has not passed. */
    @Transactional
    public void logout(AuthUser caller) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        revokedTokens.deleteExpired(now);              // housekeeping: expired tokens are rejected by signature anyway
        revokedTokens.save(new RevokedToken(caller.jti(), LocalDateTime.ofInstant(caller.tokenExpiresAt(), ZoneOffset.UTC)));
    }

    @Transactional(readOnly = true)
    public UserResponse me(AuthUser caller) {
        return users.findById(caller.id()).map(UserResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    private static ApiException emailTaken() {
        return new ApiException(HttpStatus.CONFLICT, "An account with this email already exists",
                Map.of("email", "An account with this email already exists"));
    }
}
