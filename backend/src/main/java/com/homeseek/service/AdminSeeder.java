package com.homeseek.service;

import com.homeseek.config.AppProperties;
import com.homeseek.model.AuthProvider;
import com.homeseek.model.Role;
import com.homeseek.model.User;
import com.homeseek.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * FR-003: Admin accounts are never created through public registration. The first Admin is created here from
 * ADMIN_EMAIL / ADMIN_PASSWORD at startup if no account with that email exists. Nothing is created when they are unset.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AdminSeeder(AppProperties props, UserRepository users, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = props.admin().email();
        String password = props.admin().password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set: no Admin account seeded");
            return;
        }
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[\\x20-\\x7E]{8,72}$")) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must be 8 to 72 standard characters with at least one letter and one number");
        }
        String normalized = AuthService.normalizeEmail(email);
        if (users.existsByEmail(normalized)) {
            return;
        }
        User admin = new User();
        admin.setEmail(normalized);
        admin.setFullName(props.admin().fullName());
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setAuthProvider(AuthProvider.LOCAL);
        users.save(admin);
        log.info("Seeded Admin account {}", normalized);
    }
}
