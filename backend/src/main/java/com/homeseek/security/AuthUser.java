package com.homeseek.security;

import com.homeseek.model.Role;
import java.time.Instant;

/** The authenticated caller, built from a verified token and the current database row. */
public record AuthUser(Long id, String email, Role role, String jti, Instant tokenExpiresAt) {
}
