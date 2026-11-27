package com.homeseek.service;

import com.homeseek.dto.ProfileUpdateRequest;
import com.homeseek.dto.UserResponse;
import com.homeseek.exception.ApiException;
import com.homeseek.model.User;
import com.homeseek.repository.UserRepository;
import com.homeseek.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-008: a user views and edits their own profile. The row is always found from the authenticated caller, never from
 * a request value, so there is no way to read or change another user's profile through this service.
 */
@Service
public class ProfileService {

    private final UserRepository users;

    public ProfileService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResponse get(AuthUser caller) {
        return UserResponse.from(find(caller));
    }

    @Transactional
    public UserResponse update(AuthUser caller, ProfileUpdateRequest req) {
        User user = find(caller);
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone().trim());
        return UserResponse.from(users.save(user));
    }

    private User find(AuthUser caller) {
        return users.findById(caller.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
}
