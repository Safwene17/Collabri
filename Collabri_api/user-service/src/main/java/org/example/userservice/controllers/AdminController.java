// file: src/main/java/org/example/userservice/controllers/AdminController.java
package org.example.userservice.controllers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.AdminRequest;
import org.example.userservice.dto.AdminResponse;  // ADDED
import org.example.userservice.dto.ApiResponse;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.services.AdminService;
import org.springframework.data.domain.Page;  // ADDED
import org.springframework.data.domain.Pageable;  // ADDED
import org.springframework.data.web.PageableDefault;  // ADDED
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admins")
public class AdminController {
    private final AdminService adminService;

    //------------------------------ADMIN CRUD--------------------------------//

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createAdmin(@RequestBody @Valid AdminRequest adminRequest) {
        adminService.createAdmin(adminRequest);
        return ResponseEntity.status(201).body(ApiResponse.ok("Admin created successfully", null));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")  // ADDED: Secure endpoint
    public ResponseEntity<ApiResponse<Page<AdminResponse>>> getAllAdmins(
            @PageableDefault(size = 20, sort = "email") Pageable pageable  // ADDED: Default pagination
    ) {
        Page<AdminResponse> admins = adminService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Admins retrieved successfully", admins));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")  // ADDED: Secure endpoint
    public ResponseEntity<ApiResponse<AdminResponse>> getAdminById(@PathVariable UUID id) {
        AdminResponse admin = adminService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok("Admin retrieved successfully", admin));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")  // ADDED: Secure endpoint
    public ResponseEntity<ApiResponse<Void>> updateAdmin(
            @PathVariable UUID id,
            @RequestBody @Valid AdminRequest request
    ) {
        adminService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Admin updated successfully", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")  // ADDED: Secure endpoint
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(@PathVariable UUID id) {
        adminService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Admin deleted successfully", null));
    }

    //------------------------------ADMIN AUTHENTICATION--------------------------------//

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response
    ) {
        LoginResponse login = adminService.login(request, response);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", login));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            throw new CustomException("Refresh token missing", HttpStatus.UNAUTHORIZED);
        }

        LoginResponse login = adminService.refresh(refreshTokenCookie, response);
        return ResponseEntity.ok(ApiResponse.ok("Access token refreshed", login));
    }

    // Logout -> revoke refresh tokens and clear cookies
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshTokenCookie,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,  // NEW: Require Authorization header
            HttpServletResponse response
    ) {
        adminService.logout(refreshTokenCookie, authorizationHeader, response);  // Pass header to service for validation
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

}