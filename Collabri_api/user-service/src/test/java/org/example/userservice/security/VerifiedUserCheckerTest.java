package org.example.userservice.security;

import org.example.userservice.config.VerifiedUserChecker;
import org.example.userservice.exceptions.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VerifiedUserChecker Unit Tests")
class VerifiedUserCheckerTest {

    private final VerifiedUserChecker checker = new VerifiedUserChecker();

    private JwtAuthenticationToken jwtAuth(List<String> roles, Boolean verified) {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("roles", roles, "verified", verified)
        );
        return new JwtAuthenticationToken(jwt);
    }

    // Protects authorization rule: only JWT auth is accepted.
    @Nested
    @DisplayName("authentication type")
    class AuthenticationType {

        @Test
        @DisplayName("should reject non-JWT authentication")
        void shouldRejectNonJwtAuthentication() {
            assertThatThrownBy(() -> checker.isVerified(new TestingAuthenticationToken("user", "pass")))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Invalid authentication token")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // Protects verification policy for admin bypass and regular users.
    @Nested
    @DisplayName("verification policy")
    class VerificationPolicy {

        @Test
        @DisplayName("should bypass verification for admin roles")
        void shouldBypassForAdminRole() {
            boolean result = checker.isVerified(jwtAuth(List.of("ROLE_ADMIN"), false));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should allow verified regular user")
        void shouldAllowVerifiedRegularUser() {
            boolean result = checker.isVerified(jwtAuth(List.of("ROLE_USER"), true));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should reject unverified regular user")
        void shouldRejectUnverifiedRegularUser() {
            assertThatThrownBy(() -> checker.isVerified(jwtAuth(List.of("ROLE_USER"), false)))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Email not verified")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}

