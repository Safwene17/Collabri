package org.example.userservice.controllers;

import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.ApiResponse;
import org.example.userservice.dto.dashboard.AdminDashboardResponse;
import org.example.userservice.dto.dashboard.SuperAdminDashboardResponse;
import org.example.userservice.services.UserDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
public class UserDashboardController {

    private final UserDashboardService userDashboardService;

    @GetMapping("/super-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SuperAdminDashboardResponse>> getSuperAdminDashboard() {
        return ResponseEntity.ok(ApiResponse.ok("Super admin dashboard retrieved successfully", userDashboardService.getSuperAdminDashboard()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard() {
        return ResponseEntity.ok(ApiResponse.ok("Admin dashboard retrieved successfully", userDashboardService.getAdminDashboard()));
    }
}

