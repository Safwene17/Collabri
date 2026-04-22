package org.example.userservice.service;

import org.example.userservice.entities.RefreshToken;
import org.example.userservice.entities.User;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.jwt.RefreshTokenService;
import org.example.userservice.repositories.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Unit Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .password("encoded")
                .firstname("John")
                .lastname("Doe")
                .build();
    }

    // Protects token-issuance rules (token shape, owner, expiry, revoked flag).
    @Nested
    @DisplayName("createRefreshToken()")
    class CreateRefreshToken {

        @Test
        @DisplayName("should create a non-revoked token with expiry and persist it")
        void shouldCreateAndPersistToken() {
            User user = buildUser();
            ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationMs", 60_000L);

            when(refreshTokenRepository.save(org.mockito.ArgumentMatchers.any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Instant before = Instant.now();
            RefreshToken created = refreshTokenService.createRefreshToken(user);
            Instant after = Instant.now();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            RefreshToken saved = captor.getValue();

            assertThat(saved.getToken()).isNotBlank();
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.isRevoked()).isFalse();
            assertThat(saved.getExpiresAt()).isAfterOrEqualTo(before.plusMillis(60_000L));
            assertThat(saved.getExpiresAt()).isBeforeOrEqualTo(after.plusMillis(60_000L));

            assertThat(created).isSameAs(saved);
        }
    }

    // Protects security gate that invalid/expired tokens are deleted and rejected.
    @Nested
    @DisplayName("verifyExpiration()")
    class VerifyExpiration {

        @Test
        @DisplayName("should throw unauthorized and delete token when token is expired")
        void shouldThrowWhenExpired() {
            RefreshToken token = RefreshToken.builder()
                    .token("t1")
                    .expiresAt(Instant.now().minusSeconds(1))
                    .revoked(false)
                    .build();

            assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Refresh token expired or revoked");

            verify(refreshTokenRepository).delete(token);
        }

        @Test
        @DisplayName("should throw unauthorized and delete token when token is revoked")
        void shouldThrowWhenRevoked() {
            RefreshToken token = RefreshToken.builder()
                    .token("t2")
                    .expiresAt(Instant.now().plusSeconds(60))
                    .revoked(true)
                    .build();

            assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Refresh token expired or revoked");

            verify(refreshTokenRepository).delete(token);
        }

        @Test
        @DisplayName("should not interact with repository for valid non-revoked token")
        void shouldDoNothingWhenValid() {
            RefreshToken token = RefreshToken.builder()
                    .token("t3")
                    .expiresAt(Instant.now().plusSeconds(120))
                    .revoked(false)
                    .build();

            refreshTokenService.verifyExpiration(token);

            verifyNoInteractions(refreshTokenRepository);
        }
    }

    // Protects revocation path to ensure token state flips and is persisted.
    @Nested
    @DisplayName("revokeToken()")
    class RevokeToken {

        @Test
        @DisplayName("should mark token revoked and save it")
        void shouldMarkRevokedAndSave() {
            RefreshToken token = RefreshToken.builder()
                    .token("t4")
                    .expiresAt(Instant.now().plusSeconds(60))
                    .revoked(false)
                    .build();

            refreshTokenService.revokeToken(token);

            assertThat(token.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(token);
        }
    }

    // Protects account-wide logout semantics by deleting all user refresh tokens.
    @Nested
    @DisplayName("revokeAllTokensForUser()")
    class RevokeAllTokensForUser {

        @Test
        @DisplayName("should delete all tokens owned by the user")
        void shouldDeleteAllByUser() {
            User user = buildUser();

            refreshTokenService.revokeAllTokensForUser(user);

            verify(refreshTokenRepository).deleteAllByUser(user);
        }
    }

    // Protects repository lookup contract used by refresh and logout flows.
    @Nested
    @DisplayName("findByToken()")
    class FindByToken {

        @Test
        @DisplayName("should return token when repository finds it")
        void shouldReturnTokenWhenFound() {
            RefreshToken token = RefreshToken.builder()
                    .token("found-token")
                    .expiresAt(Instant.now().plusSeconds(60))
                    .revoked(false)
                    .build();
            when(refreshTokenRepository.findByToken("found-token")).thenReturn(Optional.of(token));

            RefreshToken result = refreshTokenService.findByToken("found-token");

            assertThat(result).isSameAs(token);
        }

        @Test
        @DisplayName("should throw unauthorized when token is missing")
        void shouldThrowWhenNotFound() {
            when(refreshTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.findByToken("missing-token"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Refresh token not found")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}

