package org.example.userservice.service;

import org.example.userservice.dto.AdminDashboardResponse;
import org.example.userservice.dto.SuperAdminDashboardResponse;
import org.example.userservice.enums.Role;
import org.example.userservice.repositories.RefreshTokenRepository;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.UserDashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDashboardService Unit Tests")
class UserDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserDashboardService userDashboardService;

    // Protects admin dashboard aggregate math and verification-rate branch.
    @Nested
    @DisplayName("getAdminDashboard()")
    class GetAdminDashboard {

        @Test
        @DisplayName("should compute dashboard values when users exist")
        void shouldComputeValues() {
            when(userRepository.countByRole(Role.USER)).thenReturn(10L);
            when(userRepository.countByVerifiedTrue()).thenReturn(7L);
            when(userRepository.countByVerifiedFalse()).thenReturn(3L);
            when(userRepository.countByCreatedAtAfter(any())).thenReturn(2L, 8L);

            AdminDashboardResponse response = userDashboardService.getAdminDashboard();

            assertThat(response.totalUsers()).isEqualTo(10L);
            assertThat(response.verifiedUsers()).isEqualTo(7L);
            assertThat(response.unverifiedUsers()).isEqualTo(3L);
            assertThat(response.verificationRate()).isEqualTo(70.0d);
            assertThat(response.newUsersLast7Days()).isEqualTo(2L);
            assertThat(response.newUsersLast30Days()).isEqualTo(8L);
        }

        @Test
        @DisplayName("should return zero verification rate when total users is zero")
        void shouldReturnZeroRateWhenNoUsers() {
            when(userRepository.countByRole(Role.USER)).thenReturn(0L);
            when(userRepository.countByVerifiedTrue()).thenReturn(0L);
            when(userRepository.countByVerifiedFalse()).thenReturn(0L);
            when(userRepository.countByCreatedAtAfter(any())).thenReturn(0L, 0L);

            AdminDashboardResponse response = userDashboardService.getAdminDashboard();

            assertThat(response.verificationRate()).isEqualTo(0.0d);
        }
    }

    // Protects super-admin dashboard aggregation and grouped role mapping resilience.
    @Nested
    @DisplayName("getSuperAdminDashboard()")
    class GetSuperAdminDashboard {

        @Test
        @DisplayName("should aggregate all counters and map grouped roles")
        void shouldBuildSuperAdminDashboard() {
            when(userRepository.countByRole(Role.USER)).thenReturn(12L);
            when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);
            when(userRepository.countByVerifiedTrue()).thenReturn(9L);
            when(userRepository.countByVerifiedFalse()).thenReturn(3L);
            when(userRepository.countByCreatedAtAfter(any())).thenReturn(4L, 10L);
            when(refreshTokenRepository.countByRevokedFalseAndExpiresAtAfter(any())).thenReturn(6L);
            when(refreshTokenRepository.countByRevokedTrue()).thenReturn(1L);
            when(userRepository.countUsersGroupedByRole()).thenReturn(List.of(
                    new Object[]{Role.USER, 12L},
                    new Object[]{Role.ADMIN, 2L}
            ));

            SuperAdminDashboardResponse response = userDashboardService.getSuperAdminDashboard();

            assertThat(response.totalUsers()).isEqualTo(12L);
            assertThat(response.totalAdmins()).isEqualTo(2L);
            assertThat(response.verificationRate()).isEqualTo(75.0d);
            assertThat(response.activeRefreshTokens()).isEqualTo(6L);
            assertThat(response.revokedTokens()).isEqualTo(1L);
            assertThat(response.usersByRole())
                    .containsEntry("USER", 12L)
                    .containsEntry("ADMIN", 2L);
        }

        @Test
        @DisplayName("should ignore malformed grouped rows")
        void shouldIgnoreMalformedRows() {
            when(userRepository.countByRole(Role.USER)).thenReturn(0L);
            when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
            when(userRepository.countByVerifiedTrue()).thenReturn(0L);
            when(userRepository.countByVerifiedFalse()).thenReturn(0L);
            when(userRepository.countByCreatedAtAfter(any())).thenReturn(0L, 0L);
            when(refreshTokenRepository.countByRevokedFalseAndExpiresAtAfter(any())).thenReturn(0L);
            when(refreshTokenRepository.countByRevokedTrue()).thenReturn(0L);
            when(userRepository.countUsersGroupedByRole()).thenReturn(List.of(
                    new Object[]{null, 5L},
                    new Object[]{Role.USER},
                    new Object[]{Role.USER, null}
            ));

            SuperAdminDashboardResponse response = userDashboardService.getSuperAdminDashboard();

            assertThat(response.usersByRole()).isEmpty();
        }
    }
}

