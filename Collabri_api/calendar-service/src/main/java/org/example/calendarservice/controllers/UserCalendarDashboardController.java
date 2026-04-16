package org.example.calendarservice.controllers;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.dto.ApiResponse;
import org.example.calendarservice.dto.UserDashboardResponse;
import org.example.calendarservice.services.UserCalendarDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
public class UserCalendarDashboardController {

    private final UserCalendarDashboardService dashboardService;

    @GetMapping("/me")
    @PreAuthorize("@verified.isVerified(authentication)")
    public ResponseEntity<ApiResponse<UserDashboardResponse>> getMyDashboard(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("User dashboard retrieved successfully", dashboardService.getDashboard(userId)));
    }
}

