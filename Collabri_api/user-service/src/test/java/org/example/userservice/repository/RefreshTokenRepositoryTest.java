package org.example.userservice.repository;

import org.example.userservice.entities.RefreshToken;
import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.integration.AbstractIntegrationTest;
import org.example.userservice.repositories.RefreshTokenRepository;
import org.example.userservice.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("RefreshTokenRepository Tests")
class RefreshTokenRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User buildUser(String email) {
        return User.builder()
                .firstname("John")
                .lastname("Doe")
                .email(email)
                .password("encoded")
                .role(Role.USER)
                .verified(true)
                .build();
    }

    private RefreshToken buildToken(User user, String tokenValue, boolean revoked, Instant expiresAt) {
        return RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .revoked(revoked)
                .expiresAt(expiresAt)
                .build();
    }

    // Protects auth-critical token lookup used by refresh/logout paths.
    @Nested
    @DisplayName("findByToken")
    class FindByToken {

        @Test
        @DisplayName("should return token when exact token exists")
        void shouldReturnTokenWhenExists() {
            User user = userRepository.saveAndFlush(buildUser("rt-find@example.com"));
            refreshTokenRepository.saveAndFlush(buildToken(user, "token-find", false, Instant.now().plusSeconds(300)));

            assertThat(refreshTokenRepository.findByToken("token-find")).isPresent();
        }

        @Test
        @DisplayName("should return empty when token is unknown")
        void shouldReturnEmptyWhenMissing() {
            assertThat(refreshTokenRepository.findByToken("missing")).isEmpty();
        }
    }

    // Protects dashboard/auth metrics based on non-revoked and non-expired tokens.
    @Nested
    @DisplayName("countByRevokedFalseAndExpiresAtAfter")
    class CountActiveTokens {

        @Test
        @DisplayName("should count only active tokens")
        void shouldCountOnlyActiveTokens() {
            User user = userRepository.saveAndFlush(buildUser("rt-count@example.com"));
            Instant now = Instant.now();

            refreshTokenRepository.saveAndFlush(buildToken(user, "active-1", false, now.plusSeconds(300)));
            refreshTokenRepository.saveAndFlush(buildToken(user, "active-2", false, now.plusSeconds(1)));
            refreshTokenRepository.saveAndFlush(buildToken(user, "revoked", true, now.plusSeconds(300)));
            refreshTokenRepository.saveAndFlush(buildToken(user, "expired", false, now.minusSeconds(1)));

            long count = refreshTokenRepository.countByRevokedFalseAndExpiresAtAfter(now);

            assertThat(count).isEqualTo(2);
        }
    }

    // Protects cleanup path that revokes all sessions for a user.
    @Nested
    @DisplayName("deleteAllByUser")
    class DeleteAllByUser {

        @Test
        @DisplayName("should delete only tokens belonging to provided user")
        void shouldDeleteOnlyProvidedUserTokens() {
            User owner = userRepository.saveAndFlush(buildUser("owner@example.com"));
            User other = userRepository.saveAndFlush(buildUser("other@example.com"));

            refreshTokenRepository.saveAndFlush(buildToken(owner, "owner-1", false, Instant.now().plusSeconds(50)));
            refreshTokenRepository.saveAndFlush(buildToken(owner, "owner-2", false, Instant.now().plusSeconds(60)));
            refreshTokenRepository.saveAndFlush(buildToken(other, "other-1", false, Instant.now().plusSeconds(70)));

            refreshTokenRepository.deleteAllByUser(owner);
            refreshTokenRepository.flush();

            assertThat(refreshTokenRepository.findByToken("owner-1")).isEmpty();
            assertThat(refreshTokenRepository.findByToken("owner-2")).isEmpty();
            assertThat(refreshTokenRepository.findByToken("other-1")).isPresent();
        }
    }

    // Protects DB uniqueness of raw refresh token values.
    @Nested
    @DisplayName("unique token constraint")
    class UniqueTokenConstraint {

        @Test
        @DisplayName("should throw DataIntegrityViolationException for duplicate token value")
        void shouldEnforceUniqueTokenConstraint() {
            User user1 = userRepository.saveAndFlush(buildUser("dup1@example.com"));
            User user2 = userRepository.saveAndFlush(buildUser("dup2@example.com"));

            refreshTokenRepository.saveAndFlush(buildToken(user1, "same-token", false, Instant.now().plusSeconds(100)));

            assertThatThrownBy(() -> {
                refreshTokenRepository.save(buildToken(user2, "same-token", false, Instant.now().plusSeconds(200)));
                refreshTokenRepository.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}

