package com.homeseek.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the "homeseek.*" settings in application.yml (values come from .env / environment). */
@ConfigurationProperties(prefix = "homeseek")
public record AppProperties(Jwt jwt, Cors cors, Admin admin) {

    public record Jwt(String secret, long expirationHours) {}

    public record Cors(String allowedOrigin) {}

    public record Admin(String email, String password, String fullName) {}
}
