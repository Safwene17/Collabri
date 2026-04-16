package org.example.userservice.dto.dashboard;

import java.util.Map;

public record SuperAdminDashboardResponse(
        long totalUsers,
        long totalAdmins,
        long verifiedUsers,
        long unverifiedUsers,
        double verificationRate,
        long newUsersLast7Days,
        long newUsersLast30Days,
        long activeRefreshTokens,
        long revokedTokens,
        Map<String, Long> usersByRole
) {
}

