package org.example.userservice.repository;

import org.example.userservice.entities.PasswordResetToken;
import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.integration.AbstractIntegrationTest;
import org.example.userservice.repositories.PasswordResetTokenRepository;
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
@DisplayName("PasswordResetTokenRepository Tests")
class PasswordResetTokenRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

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

    private PasswordResetToken buildToken(User user, String tokenHash) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(Instant.now().plusSeconds(180));
        token.setUsed(false);
        return token;
    }

    // Protects auth-critical lookup for reset token validation flow.
    @Nested
    @DisplayName("findByTokenHash")
    class FindByTokenHash {

        @Test
        @DisplayName("should return token when hash exists")
        void shouldReturnTokenWhenHashExists() {
            User user = userRepository.saveAndFlush(buildUser("prt-find@example.com"));
            passwordResetTokenRepository.saveAndFlush(buildToken(user, "hash-1"));

            assertThat(passwordResetTokenRepository.findByTokenHash("hash-1")).isPresent();
        }

        @Test
        @DisplayName("should return empty when hash does not exist")
        void shouldReturnEmptyWhenHashMissing() {
            assertThat(passwordResetTokenRepository.findByTokenHash("missing")).isEmpty();
        }
    }

    // Protects cleanup behavior after successful reset or account operations.
    @Nested
    @DisplayName("deleteAllByUser")
    class DeleteAllByUser {

        @Test
        @DisplayName("should remove only tokens of the provided user")
        void shouldDeleteOnlyProvidedUsersTokens() {
            User owner = userRepository.saveAndFlush(buildUser("prt-owner@example.com"));
            User other = userRepository.saveAndFlush(buildUser("prt-other@example.com"));

            passwordResetTokenRepository.saveAndFlush(buildToken(owner, "owner-h1"));
            passwordResetTokenRepository.saveAndFlush(buildToken(owner, "owner-h2"));
            passwordResetTokenRepository.saveAndFlush(buildToken(other, "other-h1"));

            passwordResetTokenRepository.deleteAllByUser(owner);
            passwordResetTokenRepository.flush();

            assertThat(passwordResetTokenRepository.findByTokenHash("owner-h1")).isEmpty();
            assertThat(passwordResetTokenRepository.findByTokenHash("owner-h2")).isEmpty();
            assertThat(passwordResetTokenRepository.findByTokenHash("other-h1")).isPresent();
        }
    }

    // Protects uniqueness of token hashes to prevent ambiguous token ownership.
    @Nested
    @DisplayName("unique tokenHash constraint")
    class UniqueTokenHashConstraint {

        @Test
        @DisplayName("should enforce unique tokenHash at DB level")
        void shouldEnforceUniqueTokenHash() {
            User u1 = userRepository.saveAndFlush(buildUser("prt-dup1@example.com"));
            User u2 = userRepository.saveAndFlush(buildUser("prt-dup2@example.com"));

            passwordResetTokenRepository.saveAndFlush(buildToken(u1, "same-hash"));

            assertThatThrownBy(() -> {
                passwordResetTokenRepository.save(buildToken(u2, "same-hash"));
                passwordResetTokenRepository.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}

