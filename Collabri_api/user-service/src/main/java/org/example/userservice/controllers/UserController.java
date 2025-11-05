package org.example.userservice.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.*;
import org.example.userservice.services.CustomUserDetailsService;
import org.example.userservice.services.JwtService;
import org.example.userservice.services.PasswordResetService;
import org.example.userservice.services.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private final PasswordResetService passwordResetService;
//    @Value("${security.cookies.same-site:Lax}")
//    private String sameSite;
//    private final JwtService jwtService;
//    private final UserService userService;
//    private final CustomUserDetailsService customUserDetailsService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }


//    @PostMapping("/refresh")
//    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
//        // read refresh token from cookie
//        String refreshToken = null;
//        if (request.getCookies() != null) {
//            for (var c : request.getCookies()) {
//                if ("REFRESH_TOKEN".equals(c.getName())) {
//                    refreshToken = c.getValue();
//                    break;
//                }
//            }
//        }
//
//        if (refreshToken == null) {
//            return ResponseEntity.status(401).build();
//        }
//
//        try {
//            String username = jwtService.extractUsername(refreshToken);
//            var userDetails = customUserDetailsService.loadUserByUsername(username); // we will add a method to UserService
//            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
//                return ResponseEntity.status(401).build();
//            }
//
//            String newAccess = jwtService.generateAccessToken(userDetails);
//
//            ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", newAccess)
//                    .httpOnly(true)
//                    .secure(false)
//                    .path("/")
//                    .maxAge(jwtService.getAccessTokenExpirationSeconds())
//                    .sameSite(sameSite)
//                    .build();
//
//            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
//            return ResponseEntity.ok().build();
//        } catch (Exception ex) {
//            return ResponseEntity.status(401).build();
//        }
//    }

//    @PostMapping("/logout")
//    public ResponseEntity<?> logout(HttpServletResponse response) {
//        ResponseCookie clearAccess = ResponseCookie.from("ACCESS_TOKEN", "")
//                .httpOnly(true)
//                .secure(true)
//                .path("/")
//                .maxAge(0)
//                .sameSite(sameSite)
//                .build();
//        ResponseCookie clearRefresh = ResponseCookie.from("REFRESH_TOKEN", "")
//                .httpOnly(true)
//                .secure(true)
//                .path("/")
//                .maxAge(0)
//                .sameSite(sameSite)
//                .build();
//        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
//        response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());
//        return ResponseEntity.noContent().build();
//    }

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
//    @GetMapping("/validate-reset-token")
//    public ResponseEntity<Boolean> validateResetToken(@RequestParam("token") String token) {
//        boolean ok = passwordResetService.validateToken(token);
//        return ResponseEntity.ok(ok);
//    }

}
