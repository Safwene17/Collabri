package org.example.userservice.controllers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.*;
import org.example.userservice.entities.RefreshToken;
import org.example.userservice.entities.User;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.services.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final EmailVerificationService emailVerificationService;
    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final org.example.userservice.repositories.UserRepository userRepository;
    private final TokenService tokenService;

    // ✅ Register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody @Valid RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.status(201).body(
                ApiResponse.ok("Verification email sent", null)
        );
    }

    // ✅ Login (wrapped)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response
    ) {
        // Authenticate user
        userService.authenticate(request);

        // Fetch user and details
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        // Issue tokens
        String accessToken = tokenService.issueTokens(user, userDetails, response, false);

        // Return access token as string in response
        return ResponseEntity.ok(ApiResponse.ok("Login successful", new LoginResponse(accessToken)));
    }


    // ✅ Delete User
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable("id") UUID id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully", null));
    }

    // ✅ Get User by ID
    @GetMapping("/get/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable("id") UUID id) {
        UserResponse user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok("User fetched successfully", user));
    }

    // ✅ Get User by Email
    @GetMapping("/by-email")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@RequestParam("email") String email) {
        UserResponse user = userService.findByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok("User fetched successfully", user));
    }

    // 🆕 Pagination (Default page = 0, size = 5)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        PageResponse<UserResponse> users = userService.findAllPaginated(page, size);
        return ResponseEntity.ok(ApiResponse.ok("Users fetched successfully", users));
    }

    // ✅ Update user
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<Void>> updateUser(@PathVariable UUID id,
                                                        @RequestBody @Valid RegisterRequest request) {
        userService.update(id, request);
        return ResponseEntity.accepted().body(ApiResponse.ok("User updated successfully", null));
    }

    // ✅ Forgot Password
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        passwordResetService.createAndSendResetToken(request.email());
        return ResponseEntity.ok(ApiResponse.ok("A password reset link has been sent", null));
    }

    // ✅ Reset Password
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", null));
    }

    // ✅ Validate Reset Token
    @GetMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> validateResetToken(@RequestParam("token") String token) {
        boolean ok = passwordResetService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.ok(ok ? "Token valid" : "Token invalid", ok));
    }


    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestBody TokenRequest request) {
        // do not swallow exceptions here — let GlobalExceptionHandler handle them
        emailVerificationService.confirmToken(request.token());
        return ResponseEntity.ok(ApiResponse.ok("Email verified successfully", null));
    }


    // ✅ Resend verification email
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestBody @Valid ResendVerificationRequest request) {
        emailVerificationService.resendVerification(request);
        return ResponseEntity.ok(ApiResponse.ok("Verification email resent", null));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            throw new CustomException("Refresh token missing", HttpStatus.UNAUTHORIZED);
        }

        RefreshToken oldToken = refreshTokenService.findByToken(refreshTokenCookie);
        refreshTokenService.verifyExpiration(oldToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(oldToken.getUser().getEmail());
        String newAccessToken = tokenService.rotateRefreshToken(oldToken, userDetails, response, false);

        return ResponseEntity.ok(ApiResponse.ok("Access token refreshed", new LoginResponse(newAccessToken)));
    }


    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        if (refreshTokenCookie != null && !refreshTokenCookie.isBlank()) {
            try {
                RefreshToken token = refreshTokenService.findByToken(refreshTokenCookie);
                tokenService.clearRefreshToken(token.getUser(), response, false);
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }


}

