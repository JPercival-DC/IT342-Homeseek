package com.homeseek.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** FR-001 / FR-002: registration fields and password policy. */
public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be 2 to 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Size(max = 150, message = "Email must be at most 150 characters")
        @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Enter a valid email address")
        String email,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9][0-9 \\-]{6,18}[0-9]$", message = "Enter a valid phone number (8 to 20 digits)")
        String phone,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = "^[\\x20-\\x7E]*$", message = "Password may only use standard keyboard characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "Password must contain at least one letter and one number")
        String password,

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "TENANT|OWNER", message = "Role must be TENANT or OWNER")
        String role) {
}
