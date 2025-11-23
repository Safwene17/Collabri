package org.example.userservice.controllers;

import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.AdminRequest;
import org.example.userservice.dto.ApiResponse;
import org.example.userservice.services.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Void>> createAdmin(AdminRequest adminRequest) {
        adminService.createAdmin(adminRequest);
        return ResponseEntity.status(201).body(ApiResponse.ok("Admin created successfully", null));
    }

}
