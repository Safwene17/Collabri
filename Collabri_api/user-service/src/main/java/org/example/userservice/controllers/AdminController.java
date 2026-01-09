package org.example.userservice.controllers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.AdminRequest;
import org.example.userservice.dto.ApiResponse;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.services.AdminService;
import org.example.userservice.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admins")
public class AdminController {
    private final AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createAdmin(@RequestBody @Valid AdminRequest adminRequest) {
        adminService.createAdmin(adminRequest);
        return ResponseEntity.status(201).body(ApiResponse.ok("Admin created successfully", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response
    ) {
        LoginResponse login = adminService.login(request, response);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", login));
    }


}
