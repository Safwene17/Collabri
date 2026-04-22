package org.example.userservice.repository;

import org.example.userservice.entities.EmailVerificationToken;
import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.integration.AbstractIntegrationTest;
import org.example.userservice.repositories.EmailVerificationTokenRepository;
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
@DisplayName("EmailVerificationTokenRepository Tests")
class EmailVerificationTokenRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User buildUser(String email) {
        return User.builder()
                .firstname("John")
                .lastname("Doe")
                .email(email)
                .password("encoded")
                .role(Role.USER)
                .verified(false)
                .build();
    }

    private EmailVerificationToken buildToken(User user, String tokenHash) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(Instant.now().plusSeconds(180));
        token.setUsed(false);
        return token;
    }

    // Protects auth-critical verification token lookup used during email confirm.
    @Nested
    @DisplayName("findByTokenHash")
    class FindByTokenHash {

        @Test
        @DisplayName("should return token when hash exists")
        void shouldReturnTokenWhenHashExists() {
            User user = userRepository.saveAndFlush(buildUser("evt-find@example.com"));
            emailVerificationTokenRepository.saveAndFlush(buildToken(user, "v-hash-1"));

            assertThat(emailVerificationTokenRepository.findByTokenHash("v-hash-1")).isPresent();
        }

        @Test
        @DisplayName("should return empty when hash does not exist")
        void shouldReturnEmptyWhenHashMissing() {
            assertThat(emailVerificationTokenRepository.findByTokenHash("missing")).isEmpty();
        }
    }

    // Protects resend/cleanup behavior by removing all tokens for one user only.
    @Nested
    @DisplayName("deleteAllByUser")
    class DeleteAllByUser {

        @Test
        @DisplayName("should delete only tokens belonging to provided user")
        void shouldDeleteOnlyProvidedUsersTokens() {
            User owner = userRepository.saveAndFlush(buildUser("evt-owner@example.com"));
            User other = userRepository.saveAndFlush(buildUser("evt-other@example.com"));

            emailVerificationTokenRepository.saveAndFlush(buildToken(owner, "owner-v1"));
            emailVerificationTokenRepository.saveAndFlush(buildToken(owner, "owner-v2"));
            emailVerificationTokenRepository.saveAndFlush(buildToken(other, "other-v1"));

            emailVerificationTokenRepository.deleteAllByUser(owner);
            emailVerificationTokenRepository.flush();

            assertThat(emailVerificationTokenRepository.findByTokenHash("owner-v1")).isEmpty();
            assertThat(emailVerificationTokenRepository.findByTokenHash("owner-v2")).isEmpty();
            assertThat(emailVerificationTokenRepository.findByTokenHash("other-v1")).isPresent();
        }
    }

    // Protects uniqueness of verification hashes across all users.
    @Nested
    @DisplayName("unique tokenHash constraint")
    class UniqueTokenHashConstraint {

        @Test
        @DisplayName("should enforce unique tokenHash at DB level")
        void shouldEnforceUniqueTokenHash() {
            User u1 = userRepository.saveAndFlush(buildUser("evt-dup1@example.com"));
            User u2 = userRepository.saveAndFlush(buildUser("evt-dup2@example.com"));

            emailVerificationTokenRepository.saveAndFlush(buildToken(u1, "same-v-hash"));

            assertThatThrownBy(() -> {
                emailVerificationTokenRepository.save(buildToken(u2, "same-v-hash"));
                emailVerificationTokenRepository.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}

