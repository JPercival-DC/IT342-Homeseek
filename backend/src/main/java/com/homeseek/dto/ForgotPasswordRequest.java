package com.homeseek.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required")
        @Size(max = 150, message = "Email must be at most 150 characters")
        @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Enter a valid email address")
        String email) {
}
