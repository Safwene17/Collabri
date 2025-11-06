package org.example.userservice.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.*;
import org.example.userservice.services.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final EmailVerificationService emailVerificationService;
    private final UserService service;
    private final PasswordResetService passwordResetService;

    @Value("${app.frontend.verify-success-url}")
    private String frontendVerifySuccessUrl;

    @Value("${app.frontend.verify-expired-url}")
    private String frontendVerifyExpiredUrl;

    @Value("${app.frontend.verify-already-url}")
    private String frontendVerifyAlreadyUrl;

    @Value("${app.frontend.verify-failed-url}")
    private String frontendVerifyFailedUrl;

//    private final JwtService jwtService;
//    private final UserService userService;
//    private final CustomUserDetailsService customUserDetailsService;


    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody @Valid RegisterRequest request) {
        service.register(request);
        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Verification email sent")
                        .data(null)
                        .build()
        );
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam("email") String email) {
        return ResponseEntity.ok(service.findByEmail(email));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(service.findAll());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Void> updateUser(@PathVariable UUID id, @RequestBody @Valid RegisterRequest request) {
        service.update(id, request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        // to avoid revealing whether email exists, you might always return 200
        try {
            passwordResetService.createAndSendResetToken(request.email());
        } catch (IllegalArgumentException ex) {
            // swallow exception to avoid email enumeration; still return 200
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    //optionnal for frontend to validate token before showing reset form
    @GetMapping("/validate-reset-token")
    public ResponseEntity<Boolean> validateResetToken(@RequestParam("token") String token) {
        boolean ok = passwordResetService.validateToken(token);
        return ResponseEntity.ok(ok);
    }

    @GetMapping("/verify-email")
    public void verifyEmail(@RequestParam("token") String token, HttpServletResponse response) throws IOException {
        try {
            emailVerificationService.confirmToken(token);
            // success redirect (frontend page shows success message)
            response.sendRedirect(frontendVerifySuccessUrl);
        } catch (IllegalArgumentException ex) {
            // rely on message text from confirmToken to choose redirect
            String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if (msg.contains("expired")) {
                response.sendRedirect(frontendVerifyExpiredUrl);
            } else if (msg.contains("already")) {
                response.sendRedirect(frontendVerifyAlreadyUrl);
            } else {
                response.sendRedirect(frontendVerifyFailedUrl);
            }
        }
    }


    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerification(request);
        return ResponseEntity.ok().build();
    }

}
