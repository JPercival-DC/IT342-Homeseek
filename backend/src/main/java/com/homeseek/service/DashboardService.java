package com.homeseek.service;

import com.homeseek.dto.DashboardResponse;
import com.homeseek.exception.ApiException;
import com.homeseek.model.Role;
import com.homeseek.model.User;
import com.homeseek.repository.UserRepository;
import com.homeseek.security.AuthUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the role dashboards. The module lists mirror the SRS features each role will get in later increments;
 * only the Admin summary numbers are live data for now.
 */
@Service
public class DashboardService {

    private static final Map<Role, List<String>> MODULES = Map.of(
            Role.TENANT, List.of("Search and Filter Listings", "Saved Favorites", "Messages", "Booking Requests", "Notifications"),
            Role.OWNER, List.of("My Listings", "Rooms and Availability", "Messages", "Booking Requests", "Notifications"),
            Role.ADMIN, List.of("Verification Queue", "User Management"));

    private final UserRepository users;

    public DashboardService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public DashboardResponse forCaller(AuthUser caller) {
        User user = users.findById(caller.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        Map<String, Long> stats = null;
        if (user.getRole() == Role.ADMIN) {
            stats = new LinkedHashMap<>();
            stats.put("totalUsers", users.count());
            stats.put("tenants", users.countByRole(Role.TENANT));
            stats.put("owners", users.countByRole(Role.OWNER));
            stats.put("admins", users.countByRole(Role.ADMIN));
            stats.put("activeUsers", users.countByActive(true));
        }
        return new DashboardResponse(user.getRole(), "Welcome, " + user.getFullName(), MODULES.get(user.getRole()), stats);
    }
}
