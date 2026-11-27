package com.homeseek.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** ERD entity USER. Timestamps are stored in UTC. */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 10)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "google_id", length = 100, unique = true)
    private String googleId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Raised on every password change/reset; login tokens issued under an older version are rejected. */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(int tokenVersion) { this.tokenVersion = tokenVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
