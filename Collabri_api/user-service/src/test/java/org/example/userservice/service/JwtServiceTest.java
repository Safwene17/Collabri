package org.example.userservice.service;

import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService buildService(long expirationMs) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(service, "accessTokenExpirationMs", expirationMs);
        service.init();
        return service;
    }

    private User buildUser(Role role, boolean verified) {
        return User.builder()
                .id(java.util.UUID.randomUUID())
                .email("jwt-user@example.com")
                .password("encoded")
                .firstname("Jwt")
                .lastname("User")
                .role(role)
                .verified(verified)
                .build();
    }

    // Protects token payload contract consumed by downstream authorization checks.
    @Nested
    @DisplayName("generateAccessToken and claims")
    class GenerateAndClaims {

        @Test
        @DisplayName("should generate token and extract username")
        void shouldGenerateAndExtractUsername() {
            JwtService jwtService = buildService(120_000L);
            User user = buildUser(Role.ADMIN, true);

            String token = jwtService.generateAccessToken(user);

            assertThat(token).isNotBlank();
            assertThat(jwtService.extractUsername(token)).isEqualTo("jwt-user@example.com");
        }

        @Test
        @DisplayName("should include role and verified claims")
        void shouldIncludeRoleAndVerifiedClaims() {
            JwtService jwtService = buildService(120_000L);
            User user = buildUser(Role.USER, false);

            String token = jwtService.generateAccessToken(user);

            var roles = jwtService.extractClaim(token, claims -> claims.get("roles", java.util.List.class));
            Boolean verified = jwtService.extractClaim(token, claims -> claims.get("verified", Boolean.class));

            assertThat(roles).contains("ROLE_USER");
            assertThat(verified).isFalse();
        }
    }

    // Protects token time validation for security-sensitive expiry checks.
    @Nested
    @DisplayName("isTokenExpired")
    class IsTokenExpired {

        @Test
        @DisplayName("should return false for token with future expiration")
        void shouldReturnFalseForValidToken() {
            JwtService jwtService = buildService(60_000L);
            String token = jwtService.generateAccessToken(buildUser(Role.USER, true));

            assertThat(jwtService.isTokenExpired(token)).isFalse();
        }

        @Test
        @DisplayName("should throw ExpiredJwtException for token that is already expired")
        void shouldThrowForExpiredToken() {
            JwtService jwtService = buildService(-1L);
            String token = jwtService.generateAccessToken(buildUser(Role.USER, true));

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> jwtService.isTokenExpired(token))
                    .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
        }
    }
}
