package org.example.userservice.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshToken Entity Unit Tests")
class RefreshTokenTest {

    // Protects auth behavior: newly built refresh tokens must not start revoked.
    @Nested
    @DisplayName("Builder defaults")
    class BuilderDefaults {

        @Test
        @DisplayName("should default revoked to false when omitted")
        void shouldDefaultRevokedToFalse() {
            RefreshToken token = RefreshToken.builder()
                    .token("raw-refresh")
                    .expiresAt(Instant.now().plusSeconds(60))
                    .build();

            assertThat(token.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("should keep explicit revoked value when provided")
        void shouldKeepExplicitRevokedValue() {
            RefreshToken token = RefreshToken.builder()
                    .token("raw-refresh")
                    .expiresAt(Instant.now().plusSeconds(60))
                    .revoked(true)
                    .build();

            assertThat(token.isRevoked()).isTrue();
        }
    }
}

