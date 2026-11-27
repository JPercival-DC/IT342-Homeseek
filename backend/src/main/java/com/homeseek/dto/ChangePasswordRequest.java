package com.homeseek.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        @Size(max = 72, message = "Current password is incorrect")
        String currentPassword,

        @StrongPassword
        String newPassword) {
}
