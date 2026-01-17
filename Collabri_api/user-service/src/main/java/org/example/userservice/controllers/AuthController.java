package org.example.userservice.controllers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.*;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.services.AuthService;
import org.example.userservice.services.EmailVerificationService;
import org.example.userservice.services.PasswordResetService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    // Register (user creation + email verification sent)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(201).body(ApiResponse.ok("Verification email sent", null));
    }

    // Login -> access token returned, refresh token set as HttpOnly cookie
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response
    ) {
        LoginResponse login = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", login));
    }

    // Refresh access token using refresh token cookie
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            throw new CustomException("Refresh token missing", HttpStatus.UNAUTHORIZED);
        }

        LoginResponse login = authService.refresh(refreshTokenCookie, response);
        return ResponseEntity.ok(ApiResponse.ok("Access token refreshed", login));
    }

    // Logout -> revoke refresh tokens and clear cookies
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshTokenCookie,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,  // NEW: Require Authorization header
            HttpServletResponse response
    ) {
        authService.logout(refreshTokenCookie, authorizationHeader, response);  // Pass header to service for validation
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    // Verify email (token from frontend)
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestBody TokenRequest request) {
        emailVerificationService.confirmToken(request.token());
        return ResponseEntity.ok(ApiResponse.ok("Email verified successfully", null));
    }

    // Resend verification (silently handled in service)
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestBody @Valid ResendVerificationRequest request) {
        emailVerificationService.resendVerification(request);
        return ResponseEntity.ok(ApiResponse.ok("Verification email resent", null));
    }

    // Validate reset token (frontend helper)
    @PostMapping("/validate-password-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> validateResetToken(@RequestBody TokenRequest request) {
        boolean ok = passwordResetService.validateToken(request.token());
        return ResponseEntity.ok(ApiResponse.ok(ok ? "Token valid" : "Token invalid", ok));
    }

    @PostMapping("/validate-email-verification-token")
    public ResponseEntity<ApiResponse<Boolean>> validateEmailVerificationToken(@RequestBody TokenRequest request) {
        boolean ok = emailVerificationService.validateToken(request.token());
        return ResponseEntity.ok(ApiResponse.ok(ok ? "Token valid" : "Token invalid", ok));

    }

    // Forgot password - send reset email (200 even if email missing to avoid enumeration)
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        passwordResetService.createAndSendResetToken(request.email());
        return ResponseEntity.ok(ApiResponse.ok("A password reset link has been sent", null));
    }

    // Reset password using token
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", null));
    }
}
