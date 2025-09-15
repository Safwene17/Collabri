package org.example.userservice.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.services.CustomUserDetailsService;
import org.example.userservice.services.JwtService;
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

    @Value("${security.cookies.same-site:Lax}")
    private String sameSite;

    private final UserService service;
    private final JwtService jwtService;
    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();
        return ResponseEntity.ok(userService.findByEmail(email));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse tokens = userService.authenticate(request); // returns access+refresh strings

        // set cookies
        ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", tokens.access_token())
                .httpOnly(true)
                .secure(false) // adjust from property if needed
                .path("/")
                .maxAge(jwtService.getAccessTokenExpirationSeconds())
                .sameSite(sameSite)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", tokens.refresh_token())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(jwtService.getRefreshTokenExpirationSeconds())
                .sameSite(sameSite)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // For convenience you can return minimal user info or 204
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        // read refresh token from cookie
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (var c : request.getCookies()) {
                if ("REFRESH_TOKEN".equals(c.getName())) {
                    refreshToken = c.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            String username = jwtService.extractUsername(refreshToken);
            var userDetails = customUserDetailsService.loadUserByUsername(username); // we will add a method to UserService
            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                return ResponseEntity.status(401).build();
            }

            String newAccess = jwtService.generateAccessToken(userDetails);

            ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", newAccess)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(jwtService.getAccessTokenExpirationSeconds())
                    .sameSite(sameSite)
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie clearAccess = ResponseCookie.from("ACCESS_TOKEN", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite)
                .build();
        ResponseCookie clearRefresh = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());
        return ResponseEntity.noContent().build();
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
}
