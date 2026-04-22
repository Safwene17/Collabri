package org.example.userservice.service;

import jakarta.servlet.http.HttpServletResponse;
import org.example.userservice.entities.RefreshToken;
import org.example.userservice.entities.User;
import org.example.userservice.jwt.JwtService;
import org.example.userservice.jwt.RefreshTokenService;
import org.example.userservice.jwt.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService Unit Tests")
class TokenServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private TokenService tokenService;

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("token.user@example.com")
                .password("encoded")
                .firstname("Token")
                .lastname("User")
                .build();
    }

    // Protects access+refresh issue flow and cookie wiring.
    @Nested
    @DisplayName("issueTokens()")
    class IssueTokens {

        @Test
        @DisplayName("should generate access token, create refresh token, and set refresh cookie")
        void shouldIssueAccessAndRefreshCookie() {
            User user = buildUser();
            ReflectionTestUtils.setField(tokenService, "refreshTokenExpirationMs", 120_000L);

            when(jwtService.generateAccessToken(user)).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(user))
                    .thenReturn(RefreshToken.builder()
                            .token("refresh-value")
                            .expiresAt(Instant.now().plusSeconds(120))
                            .user(user)
                            .revoked(false)
                            .build());

            String result = tokenService.issueTokens(user, response);

            assertThat(result).isEqualTo("access-token");
            verify(jwtService).generateAccessToken(user);
            verify(refreshTokenService).createRefreshToken(user);

            ArgumentCaptor<String> headerValue = ArgumentCaptor.forClass(String.class);
            verify(response).addHeader(org.mockito.ArgumentMatchers.eq("Set-Cookie"), headerValue.capture());
            assertThat(headerValue.getValue()).contains("REFRESH_TOKEN=refresh-value");
            assertThat(headerValue.getValue()).contains("HttpOnly");
        }
    }

    // Protects rotation semantics (revoke old family before issuing new pair).
    @Nested
    @DisplayName("rotateRefreshToken()")
    class RotateRefreshToken {

        @Test
        @DisplayName("should revoke all user tokens then issue a new token pair")
        void shouldRevokeThenIssue() {
            User user = buildUser();
            RefreshToken old = RefreshToken.builder()
                    .token("old")
                    .user(user)
                    .revoked(false)
                    .expiresAt(Instant.now().plusSeconds(10))
                    .build();

            TokenService spy = org.mockito.Mockito.spy(tokenService);
            org.mockito.Mockito.doReturn("new-access").when(spy).issueTokens(user, response);

            String access = spy.rotateRefreshToken(old, user, response);

            assertThat(access).isEqualTo("new-access");
            var order = inOrder(refreshTokenService, spy);
            order.verify(refreshTokenService).revokeAllTokensForUser(user);
            order.verify(spy).issueTokens(user, response);
        }

        @Test
        @DisplayName("should throw when old token has no user")
        void shouldThrowWhenOldTokenHasNoUser() {
            RefreshToken old = RefreshToken.builder()
                    .token("old")
                    .user(null)
                    .expiresAt(Instant.now().plusSeconds(10))
                    .revoked(false)
                    .build();

            assertThatThrownBy(() -> tokenService.rotateRefreshToken(old, buildUser(), response))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot rotate refresh token without user");

            verifyNoInteractions(refreshTokenService);
        }
    }

    // Protects logout contract: revoke server tokens and clear browser cookie.
    @Nested
    @DisplayName("clearRefreshToken()")
    class ClearRefreshToken {

        @Test
        @DisplayName("should revoke all user tokens and set cookie maxAge to zero when userDetails exists")
        void shouldRevokeAndClearCookieWhenUserProvided() {
            User user = buildUser();

            tokenService.clearRefreshToken(user, response);

            verify(refreshTokenService).revokeAllTokensForUser(user);
            ArgumentCaptor<String> headerValue = ArgumentCaptor.forClass(String.class);
            verify(response).addHeader(org.mockito.ArgumentMatchers.eq("Set-Cookie"), headerValue.capture());
            assertThat(headerValue.getValue()).contains("REFRESH_TOKEN=");
            assertThat(headerValue.getValue()).contains("Max-Age=0");
        }

        @Test
        @DisplayName("should only clear cookie when userDetails is null")
        void shouldOnlyClearCookieWhenUserDetailsNull() {
            tokenService.clearRefreshToken((UserDetails) null, response);

            verifyNoInteractions(refreshTokenService);
            verify(response).addHeader(org.mockito.ArgumentMatchers.eq("Set-Cookie"), org.mockito.ArgumentMatchers.contains("Max-Age=0"));
        }
    }
}
