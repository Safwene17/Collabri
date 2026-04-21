package org.example.userservice.dto;

public record AdminDashboardResponse(
        long totalUsers,
        long verifiedUsers,
        long unverifiedUsers,
        double verificationRate,
        long newUsersLast7Days,
        long newUsersLast30Days
) {
}

