// file: src/main/java/org/example/userservice/controllers/AdminController.java
package org.example.userservice.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.*;
import org.example.userservice.services.AdminService;
import org.example.userservice.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admins")
@Tag(name = "Admin Management", description = "Endpoints for managing admin users")
public class AdminController {
    private final AdminService adminService;
    private final UserService userService;

    //------------------------------ADMIN CRUD--------------------------------//

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createAdmin(@RequestBody @Valid UserRequest request) {
        adminService.createAdmin(request);
        return ResponseEntity.status(201).body(ApiResponse.ok("Admin created successfully", null));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")  // ADDED: Secure endpoint
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllAdmins(
            @PageableDefault(size = 20, sort = "email") Pageable pageable  // ADDED: Default pagination
    ) {
        Page<UserResponse> admins = adminService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Admins retrieved successfully", admins));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")  // ADDED: Secure endpoint
    public ResponseEntity<ApiResponse<UserResponse>> getAdminById(@PathVariable UUID id) {
        UserResponse user = adminService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok("Admin retrieved successfully", user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")  // ADDED: Secure endpoint
    public ResponseEntity<ApiResponse<Void>> updateAdmin(
            @PathVariable UUID id,
            @RequestBody @Valid UserRequest request
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
}