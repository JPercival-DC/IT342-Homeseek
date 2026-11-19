package com.homeseek.dto;

import com.homeseek.model.Role;
import com.homeseek.model.User;

/** Public view of a user. Never contains the password hash. */
public record UserResponse(Long id, String fullName, String email, String phone, Role role) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole());
    }
}
