package com.homeseek.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** FR-008: the only profile fields a user may edit. Email and role are deliberately not here. */
public record ProfileUpdateRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be 2 to 100 characters")
        String fullName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9][0-9 \\-]{6,18}[0-9]$", message = "Enter a valid phone number (8 to 20 digits)")
        String phone) {
}
