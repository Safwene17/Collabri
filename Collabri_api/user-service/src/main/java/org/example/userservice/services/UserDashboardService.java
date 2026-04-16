package org.example.userservice.services;

import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.dashboard.AdminDashboardResponse;
import org.example.userservice.dto.dashboard.SuperAdminDashboardResponse;
import org.example.userservice.enums.Role;
import org.example.userservice.repositories.RefreshTokenRepository;
import org.example.userservice.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserDashboardService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    public SuperAdminDashboardResponse getSuperAdminDashboard() {
        return buildSuperAdminDashboard();
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        long totalUsers = userRepository.countByRole(Role.USER);
        long verifiedUsers = userRepository.countByVerifiedTrue();
        long unverifiedUsers = userRepository.countByVerifiedFalse();
        double verificationRate = calculateVerificationRate(totalUsers, verifiedUsers);
        long newUsersLast7Days = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(7));
        long newUsersLast30Days = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(30));

        return new AdminDashboardResponse(
                totalUsers,
                verifiedUsers,
                unverifiedUsers,
                verificationRate,
                newUsersLast7Days,
                newUsersLast30Days
        );
    }

    private SuperAdminDashboardResponse buildSuperAdminDashboard() {
        long totalUsers = userRepository.countByRole(Role.USER);
        long totalAdmins = userRepository.countByRole(Role.ADMIN);
        long verifiedUsers = userRepository.countByVerifiedTrue();
        long unverifiedUsers = userRepository.countByVerifiedFalse();
        double verificationRate = calculateVerificationRate(totalUsers, verifiedUsers);
        long newUsersLast7Days = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(7));
        long newUsersLast30Days = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(30));
        long activeRefreshTokens = refreshTokenRepository.countByRevokedFalseAndExpiresAtAfter(java.time.Instant.now());
        long revokedTokens = refreshTokenRepository.countByRevokedTrue();

        Map<String, Long> usersByRole = new HashMap<>();
        for (Object[] row : userRepository.countUsersGroupedByRole()) {
            if (row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            usersByRole.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        return new SuperAdminDashboardResponse(
                totalUsers,
                totalAdmins,
                verifiedUsers,
                unverifiedUsers,
                verificationRate,
                newUsersLast7Days,
                newUsersLast30Days,
                activeRefreshTokens,
                revokedTokens,
                usersByRole
        );
    }

    private double calculateVerificationRate(long totalUsers, long verifiedUsers) {
        if (totalUsers == 0) {
            return 0.0d;
        }
        return (verifiedUsers * 100.0d) / totalUsers;
    }
}

