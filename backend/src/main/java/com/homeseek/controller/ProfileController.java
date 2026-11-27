package com.homeseek.controller;

import com.homeseek.dto.ProfileUpdateRequest;
import com.homeseek.dto.UserResponse;
import com.homeseek.security.AuthUser;
import com.homeseek.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The caller's own profile. There is no id in the URL on purpose. Any logged-in role may use it. */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public UserResponse get(@AuthenticationPrincipal AuthUser caller) {
        return profiles.get(caller);
    }

    @PutMapping
    public UserResponse update(@AuthenticationPrincipal AuthUser caller, @Valid @RequestBody ProfileUpdateRequest request) {
        return profiles.update(caller, request);
    }
}
