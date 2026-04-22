package org.example.userservice.service;

import jakarta.servlet.http.HttpServletResponse;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.dto.UserRequest;
import org.example.userservice.entities.RefreshToken;
import org.example.userservice.entities.User;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.jwt.RefreshTokenService;
import org.example.userservice.jwt.TokenService;
import org.example.userservice.mappers.UserMapper;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.AuthService;
import org.example.userservice.services.CustomUserDetailsService;
import org.example.userservice.services.EmailVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private TokenService tokenService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthService authService;

    private User buildUser(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("encoded")
                .firstname("John")
                .lastname("Doe")
                .build();
    }

    private UserRequest buildRequest(String email) {
        return new UserRequest("John", "Doe", email, "StrongPass1!");
    }

    // Protects registration rules: unique email, password encoding, and verification dispatch.
    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should save encoded user and trigger verification email")
        void shouldRegisterSuccessfully() {
            UserRequest request = buildRequest("new@example.com");
            User mapped = buildUser("new@example.com");

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userMapper.toUser(request)).thenReturn(mapped);
            when(passwordEncoder.encode("encoded")).thenReturn("hashed");

            authService.register(request);

            verify(userRepository).save(mapped);
            verify(emailVerificationService).createAndSendVerificationToken("new@example.com");
            assertThat(mapped.getPassword()).isEqualTo("hashed");
        }

        @Test
        @DisplayName("should throw 422 when email already exists")
        void shouldThrowWhenEmailExists() {
            UserRequest request = buildRequest("dup@example.com");
            when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Email already exists")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

            verifyNoInteractions(userMapper, passwordEncoder, emailVerificationService);
        }

        @Test
        @DisplayName("should wrap mail sending failures as 500")
        void shouldWrapMailFailures() {
            UserRequest request = buildRequest("mailfail@example.com");
            User mapped = buildUser("mailfail@example.com");

            when(userRepository.existsByEmail("mailfail@example.com")).thenReturn(false);
            when(userMapper.toUser(request)).thenReturn(mapped);
            when(passwordEncoder.encode("encoded")).thenReturn("hashed");
            org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
                    .when(emailVerificationService).createAndSendVerificationToken("mailfail@example.com");

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Failed to send verification email")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Protects login flow: credentials check, user lookup, and access token issue.
    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("should authenticate and return issued access token")
        void shouldLoginSuccessfully() {
            LoginRequest request = new LoginRequest("john@example.com", "StrongPass1!");
            User user = buildUser("john@example.com");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
            when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(user);
            when(tokenService.issueTokens(user, response)).thenReturn("access-token");

            LoginResponse loginResponse = authService.login(request, response);

            assertThat(loginResponse.access_token()).isEqualTo("access-token");
            verify(authenticationManager).authenticate(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when authentication fails")
        void shouldThrowWhenCredentialsInvalid() {
            LoginRequest request = new LoginRequest("john@example.com", "bad");
            org.mockito.Mockito.doThrow(new RuntimeException("bad credentials"))
                    .when(authenticationManager).authenticate(org.mockito.ArgumentMatchers.any());

            assertThatThrownBy(() -> authService.login(request, response))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Invalid credentials")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // Protects refresh flow and invalid-token branch coverage.
    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("should verify old token and return rotated access token")
        void shouldRefreshSuccessfully() {
            User user = buildUser("refresh@example.com");
            RefreshToken old = RefreshToken.builder()
                    .token("old")
                    .user(user)
                    .revoked(false)
                    .expiresAt(Instant.now().plusSeconds(30))
                    .build();

            when(refreshTokenService.findByToken("old")).thenReturn(old);
            when(userDetailsService.loadUserByUsername("refresh@example.com")).thenReturn(user);
            when(tokenService.rotateRefreshToken(old, user, response)).thenReturn("new-access");

            LoginResponse result = authService.refresh("old", response);

            assertThat(result.access_token()).isEqualTo("new-access");
            var order = inOrder(refreshTokenService, userDetailsService, tokenService);
            order.verify(refreshTokenService).findByToken("old");
            order.verify(refreshTokenService).verifyExpiration(old);
            order.verify(userDetailsService).loadUserByUsername("refresh@example.com");
            order.verify(tokenService).rotateRefreshToken(old, user, response);
        }

        @Test
        @DisplayName("should throw UNAUTHORIZED when token has no user")
        void shouldThrowWhenTokenHasNoUser() {
            RefreshToken old = RefreshToken.builder()
                    .token("old")
                    .user(null)
                    .revoked(false)
                    .expiresAt(Instant.now().plusSeconds(30))
                    .build();
            when(refreshTokenService.findByToken("old")).thenReturn(old);

            assertThatThrownBy(() -> authService.refresh("old", response))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Invalid refresh token for user")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // Protects logout behavior for missing token and successful revoke+clear sequence.
    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("should throw UNAUTHORIZED when refresh token is blank")
        void shouldThrowWhenMissingToken() {
            assertThatThrownBy(() -> authService.logout("  ", response))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Refresh token missing")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);

            verifyNoInteractions(refreshTokenService, tokenService);
        }

        @Test
        @DisplayName("should revoke token and clear cookie when token is provided")
        void shouldRevokeAndClearCookie() {
            User user = buildUser("logout@example.com");
            RefreshToken token = RefreshToken.builder()
                    .token("rt")
                    .user(user)
                    .revoked(false)
                    .expiresAt(Instant.now().plusSeconds(40))
                    .build();
            when(refreshTokenService.findByToken("rt")).thenReturn(token);

            authService.logout("rt", response);

            verify(refreshTokenService).revokeToken(token);
            verify(tokenService).clearRefreshToken(user, response);
        }
    }
}

