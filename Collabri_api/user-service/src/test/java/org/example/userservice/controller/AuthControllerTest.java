package org.example.userservice.controller;

import org.example.userservice.config.SecurityConfigTest;
import org.example.userservice.controllers.AuthController;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.services.AuthService;
import org.example.userservice.services.EmailVerificationService;
import org.example.userservice.services.PasswordResetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfigTest.class)
@DisplayName("AuthController Web Layer Tests")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private EmailVerificationService emailVerificationService;

        @MockitoBean
        private PasswordResetService passwordResetService;

        @MockitoBean
        private JpaMetamodelMappingContext jpaMetamodelMappingContext;

        private String validUserJson() {
                return """
                                {
                                  "firstname":"John",
                                  "lastname":"Doe",
                                  "email":"john@example.com",
                                  "password":"StrongPass1!"
                                }
                                """;
        }

        // Protects public auth endpoints behavior, validation, and service error
        // translation.
        @Nested
        @DisplayName("Authentication endpoints")
        class AuthenticationEndpoints {

                @Test
                @DisplayName("should register user and return 201")
                void shouldRegister() throws Exception {
                        doNothing().when(authService).register(org.mockito.ArgumentMatchers.any());

                        mockMvc.perform(post("/api/v1/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(validUserJson()))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.message").value("Verification email sent"));

                        verify(authService).register(org.mockito.ArgumentMatchers.any());
                }

                @Test
                @DisplayName("should return 400 when register body is invalid")
                void shouldReturnBadRequestOnInvalidRegisterBody() throws Exception {
                        mockMvc.perform(post("/api/v1/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{" +
                                                        "\"firstname\":\"\"," +
                                                        "\"lastname\":\"\"," +
                                                        "\"email\":\"bad\"," +
                                                        "\"password\":\"weak\"" +
                                                        "}"))
                                        .andExpect(status().isBadRequest());

                        verifyNoInteractions(authService);
                }

                @Test
                @DisplayName("should login and return access token in response body")
                void shouldLogin() throws Exception {
                        when(authService.login(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                                        .thenReturn(new LoginResponse("access-token"));

                        mockMvc.perform(post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {
                                                          "email":"john@example.com",
                                                          "password":"StrongPass1!"
                                                        }
                                                        """))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.access_token").value("access-token"));
                }

                @Test
                @DisplayName("should return 401 when refresh token cookie is missing")
                void shouldReturnUnauthorizedWhenRefreshCookieMissing() throws Exception {
                        mockMvc.perform(post("/api/v1/auth/refresh-token"))
                                        .andExpect(status().isUnauthorized());

                        verifyNoInteractions(authService);
                }

                @Test
                @DisplayName("should verify email token and return 200")
                void shouldVerifyEmail() throws Exception {
                        doNothing().when(emailVerificationService).confirmToken("token-123");

                        mockMvc.perform(post("/api/v1/auth/verify-email")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"token\":\"token-123\"}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Email verified successfully"));

                        verify(emailVerificationService).confirmToken("token-123");
                }

                @Test
                @DisplayName("should return token valid=true when password reset token is valid")
                void shouldReturnValidTokenResult() throws Exception {
                        when(passwordResetService.validateToken("token-123")).thenReturn(true);

                        mockMvc.perform(post("/api/v1/auth/validate-password-reset-token")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"token\":\"token-123\"}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data").value(true))
                                        .andExpect(jsonPath("$.message").value("Token valid"));
                }

                @Test
                @DisplayName("should return 400 when forgot-password body is invalid")
                void shouldReturnBadRequestForInvalidForgotPasswordBody() throws Exception {
                        mockMvc.perform(post("/api/v1/auth/forgot-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"email\":\"\"}"))
                                        .andExpect(status().isBadRequest());

                        verifyNoInteractions(passwordResetService);
                }

                @Test
                @DisplayName("should translate service custom exception for reset-password")
                void shouldTranslateServiceErrorForResetPassword() throws Exception {
                        org.mockito.Mockito.doThrow(new CustomException("Token expired", HttpStatus.BAD_REQUEST))
                                        .when(passwordResetService).resetPassword(org.mockito.ArgumentMatchers.any());

                        mockMvc.perform(post("/api/v1/auth/reset-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {
                                                          "token":"expired-token",
                                                          "newPassword":"StrongPass1!"
                                                        }
                                                        """))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message").value("Token expired"));
                }

                @Test
                @DisplayName("should refresh access token when refresh cookie is present")
                void shouldRefreshTokenWhenCookiePresent() throws Exception {
                        when(authService.refresh(org.mockito.ArgumentMatchers.eq("refresh-cookie"), org.mockito.ArgumentMatchers.any()))
                                        .thenReturn(new LoginResponse("new-access-token"));

                        mockMvc.perform(post("/api/v1/auth/refresh-token")
                                        .cookie(new Cookie("REFRESH_TOKEN", "refresh-cookie")))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Access token refreshed"))
                                        .andExpect(jsonPath("$.data.access_token").value("new-access-token"));

                        verify(authService).refresh(org.mockito.ArgumentMatchers.eq("refresh-cookie"), org.mockito.ArgumentMatchers.any());
                }

                @Test
                @DisplayName("should logout successfully even when refresh cookie is absent")
                void shouldLogoutSuccessfully() throws Exception {
                        doNothing().when(authService).logout(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any());

                        mockMvc.perform(post("/api/v1/auth/logout"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Logged out successfully"));

                        verify(authService).logout(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any());
                }

                @Test
                @DisplayName("should resend verification email for valid request")
                void shouldResendVerification() throws Exception {
                        doNothing().when(emailVerificationService)
                                        .resendVerification(new org.example.userservice.dto.ResendVerificationRequest("john@example.com"));

                        mockMvc.perform(post("/api/v1/auth/resend-verification")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"email\":\"john@example.com\"}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Verification email resent"));

                        verify(emailVerificationService)
                                        .resendVerification(new org.example.userservice.dto.ResendVerificationRequest("john@example.com"));
                }

                @Test
                @DisplayName("should return 400 when resend-verification body is invalid")
                void shouldReturnBadRequestForInvalidResendVerificationBody() throws Exception {
                        mockMvc.perform(post("/api/v1/auth/resend-verification")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"email\":\"bad-email\"}"))
                                        .andExpect(status().isBadRequest());

                        verifyNoInteractions(emailVerificationService);
                }

                @Test
                @DisplayName("should return token valid=false for invalid email verification token")
                void shouldReturnInvalidEmailVerificationTokenResult() throws Exception {
                        when(emailVerificationService.validateToken("token-123")).thenReturn(false);

                        mockMvc.perform(post("/api/v1/auth/validate-email-verification-token")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"token\":\"token-123\"}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data").value(false))
                                        .andExpect(jsonPath("$.message").value("Token invalid"));
                }

                @Test
                @DisplayName("should create forgot-password request for valid body")
                void shouldCreateForgotPasswordRequest() throws Exception {
                        doNothing().when(passwordResetService).createAndSendResetToken("john@example.com");

                        mockMvc.perform(post("/api/v1/auth/forgot-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"email\":\"john@example.com\"}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("A password reset link has been sent"));

                        verify(passwordResetService).createAndSendResetToken("john@example.com");
                }

                @Test
                @DisplayName("should reset password successfully")
                void shouldResetPasswordSuccessfully() throws Exception {
                        doNothing().when(passwordResetService).resetPassword(org.mockito.ArgumentMatchers.any());

                        mockMvc.perform(post("/api/v1/auth/reset-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {
                                                          "token":"valid-token",
                                                          "newPassword":"StrongPass1!"
                                                        }
                                                        """))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Password reset successfully"));

                        verify(passwordResetService).resetPassword(org.mockito.ArgumentMatchers.any());
                }
        }
}
