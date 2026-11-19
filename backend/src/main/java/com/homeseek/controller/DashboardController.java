package com.homeseek.controller;

import com.homeseek.dto.DashboardResponse;
import com.homeseek.security.AuthUser;
import com.homeseek.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** One endpoint per role. SecurityConfig restricts each path prefix to its role (401 / 403 otherwise). */
@RestController
public class DashboardController {

    private final DashboardService dashboards;

    public DashboardController(DashboardService dashboards) {
        this.dashboards = dashboards;
    }

    @GetMapping("/api/tenant/dashboard")
    public DashboardResponse tenant(@AuthenticationPrincipal AuthUser caller) {
        return dashboards.forCaller(caller);
    }

    @GetMapping("/api/owner/dashboard")
    public DashboardResponse owner(@AuthenticationPrincipal AuthUser caller) {
        return dashboards.forCaller(caller);
    }

    @GetMapping("/api/admin/dashboard")
    public DashboardResponse admin(@AuthenticationPrincipal AuthUser caller) {
        return dashboards.forCaller(caller);
    }
}
