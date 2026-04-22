package org.example.userservice.controller;

import org.example.userservice.config.SecurityConfigTest;
import org.example.userservice.controllers.UserDashboardController;
import org.example.userservice.dto.AdminDashboardResponse;
import org.example.userservice.dto.SuperAdminDashboardResponse;
import org.example.userservice.services.UserDashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserDashboardController.class)
@Import(SecurityConfigTest.class)
@DisplayName("UserDashboardController Web Layer Tests")
class UserDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserDashboardService userDashboardService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    // Protects role-based access matrix for admin and super-admin dashboard endpoints.
    @Nested
    @DisplayName("GET /api/v1/dashboard/super-admin")
    class SuperAdminDashboardEndpoint {

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("should return super-admin dashboard for SUPER_ADMIN")
        void shouldReturnDashboardForSuperAdmin() throws Exception {
            when(userDashboardService.getSuperAdminDashboard())
                    .thenReturn(new SuperAdminDashboardResponse(100, 5, 90, 10, 90.0, 20, 50, 12, 4, Map.of("USER", 100L)));

            mockMvc.perform(get("/api/v1/dashboard/super-admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalUsers").value(100));

            verify(userDashboardService).getSuperAdminDashboard();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 403 for ADMIN role")
        void shouldReturnForbiddenForAdminRole() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/super-admin"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(userDashboardService);
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturnUnauthorizedWhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/super-admin"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(userDashboardService);
        }
    }

    // Protects allowed-role matrix for admin dashboard endpoint.
    @Nested
    @DisplayName("GET /api/v1/dashboard/admin")
    class AdminDashboardEndpoint {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return admin dashboard for ADMIN")
        void shouldReturnDashboardForAdmin() throws Exception {
            when(userDashboardService.getAdminDashboard())
                    .thenReturn(new AdminDashboardResponse(100, 80, 20, 80.0, 15, 40));

            mockMvc.perform(get("/api/v1/dashboard/admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.verificationRate").value(80.0));

            verify(userDashboardService).getAdminDashboard();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 403 for USER role")
        void shouldReturnForbiddenForUserRole() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/admin"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(userDashboardService);
        }
    }
}

