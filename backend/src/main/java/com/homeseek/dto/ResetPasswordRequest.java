package com.homeseek.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** token comes from the link in the reset email. */
public record ResetPasswordRequest(
        @NotBlank(message = "The reset link is missing its token")
        @Size(max = 128, message = "The reset link is not valid")
        String token,

        @StrongPassword
        String newPassword) {
}
