package com.homeseek.service;

import com.homeseek.config.AppProperties;
import com.homeseek.exception.ApiException;
import com.homeseek.model.AuthProvider;
import com.homeseek.model.PasswordResetToken;
import com.homeseek.model.User;
import com.homeseek.repository.PasswordResetTokenRepository;
import com.homeseek.repository.UserRepository;
import com.homeseek.security.AuthUser;
import com.homeseek.security.LoginAttemptService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Forgot password, reset password and change password (SRS FR-008, FR-010). Kept apart from AuthService: it reuses the
 * same users table, password encoder and attempt counter, and ends older sessions by raising the user's token version.
 *
 * Reset links: a random 256-bit token is emailed (Gmail SMTP) and only its SHA-256 hash is stored. The link works once
 * and expires after homeseek.password-reset.expiration-minutes (30, per FR-010).
 */
@Service
public class PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);
    private static final String BAD_LINK = "This reset link is invalid or has expired. Please request a new one.";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final LoginAttemptService attempts;
    private final PasswordResetMailer mailer;
    private final AppProperties props;

    public PasswordService(UserRepository users, PasswordResetTokenRepository tokens, PasswordEncoder encoder,
            LoginAttemptService attempts, PasswordResetMailer mailer, AppProperties props) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.attempts = attempts;
        this.mailer = mailer;
        this.props = props;
    }

    /** SHA-256 hex of a token. A fast hash is enough here because the token is 256 random bits, not a human password. */
    public static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Emails a reset link if the account exists. The answer is the same for every email, and everything after the
     * mail-server check runs in the background, so neither the response nor its timing shows whether an account exists.
     */
    public void forgot(String rawEmail) {
        if (!mailer.configured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Password recovery is not set up on this server. Please contact the administrator.");
        }
        String email = AuthService.normalizeEmail(rawEmail);
        CompletableFuture.runAsync(() -> sendResetLink(email));
    }

    private void sendResetLink(String email) {
        try {
            User user = users.findByEmail(email).filter(PasswordService::canUsePassword).orElse(null);
            if (user == null) {
                return;
            }
            LocalDateTime now = now();
            AppProperties.PasswordReset cfg = props.passwordReset();
            tokens.deleteExpiredBefore(now.minusDays(1));
            // Cooldown: repeated requests cannot be used to flood someone's inbox.
            if (tokens.existsByUserIdAndCreatedAtAfter(user.getId(), now.minusSeconds(cfg.cooldownSeconds()))) {
                return;
            }
            tokens.markAllUsed(user.getId(), now);                  // a newer link replaces any older one
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            tokens.save(new PasswordResetToken(user.getId(), hashToken(token), now.plusMinutes(cfg.expirationMinutes()), now));
            mailer.send(user.getEmail(), props.cors().allowedOrigin() + "/reset-password?token=" + token,
                    cfg.expirationMinutes());
        } catch (RuntimeException e) {
            log.error("Could not send a password reset email: {}", e.getMessage());    // never shown to the caller
        }
    }

    /**
     * Checks the token against its stored hash, uses it up, and stores the new password. All in one transaction, so a
     * failure part-way leaves the link usable.
     */
    @Transactional
    public void reset(String token, String newPassword) {
        PasswordResetToken stored = tokens.findByTokenHash(hashToken(token))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, BAD_LINK));
        if (tokens.markUsed(stored.getTokenHash(), now()) == 0) {       // expired, already used, or used a moment ago
            throw new ApiException(HttpStatus.BAD_REQUEST, BAD_LINK);
        }
        User user = users.findById(stored.getUserId()).filter(PasswordService::canUsePassword)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, BAD_LINK));
        store(user, newPassword);
        attempts.reset(user.getEmail());                  // a user locked out by failed logins can get back in
    }

    /** Verifies the current password, then replaces it. Wrong guesses are limited like login attempts. */
    @Transactional
    public void change(AuthUser caller, String currentPassword, String newPassword) {
        String key = "pwchange:" + caller.email();
        Duration locked = attempts.remainingLock(key);
        if (!locked.isZero()) {
            long minutes = Math.max(1, (locked.getSeconds() + 59) / 60);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many incorrect attempts. Try again in " + minutes + " minute(s).");
        }
        User user = users.findById(caller.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        boolean tooLong = currentPassword.getBytes(StandardCharsets.UTF_8).length > 72;       // BCrypt limit
        if (tooLong || user.getPasswordHash() == null || !encoder.matches(currentPassword, user.getPasswordHash())) {
            attempts.recordFailure(key);
            throw fieldError("currentPassword", "Current password is incorrect");   // 400, not 401: the session is fine
        }
        if (encoder.matches(newPassword, user.getPasswordHash())) {
            throw fieldError("newPassword", "New password must be different from the current password");
        }
        store(user, newPassword);
        attempts.reset(key);
    }

    /**
     * Saves the new hash and raises the token version (every earlier login token stops working). Reset links still
     * open for this user are cancelled as well.
     */
    private void store(User user, String newPassword) {
        user.setPasswordHash(encoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        users.save(user);
        tokens.markAllUsed(user.getId(), now());
    }

    private static boolean canUsePassword(User u) {
        return u.isActive() && u.getAuthProvider() == AuthProvider.LOCAL;
    }

    private static ApiException fieldError(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, Map.of(field, message));
    }
}
