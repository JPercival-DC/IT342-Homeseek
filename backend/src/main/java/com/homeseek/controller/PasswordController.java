package com.homeseek.controller;

import com.homeseek.dto.ChangePasswordRequest;
import com.homeseek.dto.ForgotPasswordRequest;
import com.homeseek.dto.ResetPasswordRequest;
import com.homeseek.security.AuthUser;
import com.homeseek.service.PasswordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** /forgot and /reset are public (see SecurityConfig); /change needs a logged-in user. */
@RestController
@RequestMapping("/api/password")
public class PasswordController {

    private final PasswordService passwords;

    public PasswordController(PasswordService passwords) {
        this.passwords = passwords;
    }

    @PostMapping("/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        passwords.forgot(request.email());
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest request) {
        passwords.reset(request.token(), request.newPassword());
    }

    @PostMapping("/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void change(@AuthenticationPrincipal AuthUser caller, @Valid @RequestBody ChangePasswordRequest request) {
        passwords.change(caller, request.currentPassword(), request.newPassword());
    }
}
