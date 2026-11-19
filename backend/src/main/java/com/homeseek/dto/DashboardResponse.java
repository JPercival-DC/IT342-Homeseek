package com.homeseek.dto;

import com.homeseek.model.Role;
import java.util.List;
import java.util.Map;

/** Data for a role dashboard. "stats" is only filled for roles that have summary numbers (Admin). */
public record DashboardResponse(Role role, String welcome, List<String> modules, Map<String, Long> stats) {
}
