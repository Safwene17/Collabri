package org.example.userservice.controller;

import org.example.userservice.config.SecurityConfigTest;
import org.example.userservice.controllers.AdminController;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.services.AdminService;
import org.example.userservice.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfigTest.class)
@DisplayName("AdminController Web Layer Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final UUID ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private String validUserJson() {
        return """
                {
                    "firstname":"Admin",
                    "lastname":"User",
                    "email":"admin@example.com",
                    "password":"StrongPass1!"
                }
                """;
    }

    // Protects SUPER_ADMIN-only authorization boundaries and error handling on admin CRUD endpoints.
    @Nested
    @DisplayName("Admin CRUD endpoints")
    class AdminCrudEndpoints {

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("should create admin when caller is SUPER_ADMIN")
        void shouldCreateAdminAsSuperAdmin() throws Exception {
            doNothing().when(adminService).createAdmin(org.mockito.ArgumentMatchers.any());

            mockMvc.perform(post("/api/v1/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));

            verify(adminService).createAdmin(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 403 when ADMIN tries to create admin")
        void shouldReturnForbiddenForAdminRole() throws Exception {
            mockMvc.perform(post("/api/v1/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(adminService);
        }

        @Test
        @DisplayName("should return 401 for unauthenticated create admin request")
        void shouldReturnUnauthorizedWhenUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(adminService);
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("should return paginated admins for SUPER_ADMIN")
        void shouldReturnAllAdmins() throws Exception {
            Page<UserResponse> page = new PageImpl<>(List.of(new UserResponse(ADMIN_ID, "Admin", "User", "admin@example.com")));
            when(adminService.findAll(org.mockito.ArgumentMatchers.any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/admins"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].email").value("admin@example.com"));

            verify(adminService).findAll(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("should return 404 when admin not found by id")
        void shouldReturnNotFoundForMissingAdmin() throws Exception {
            when(adminService.findById(ADMIN_ID)).thenThrow(new CustomException("Admin not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/v1/admins/{id}", ADMIN_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Admin not found"));
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("should update admin when SUPER_ADMIN")
        void shouldUpdateAdmin() throws Exception {
            doNothing().when(adminService).update(org.mockito.ArgumentMatchers.eq(ADMIN_ID), org.mockito.ArgumentMatchers.any());

            mockMvc.perform(put("/api/v1/admins/{id}", ADMIN_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Admin updated successfully"));

            verify(adminService).update(org.mockito.ArgumentMatchers.eq(ADMIN_ID), org.mockito.ArgumentMatchers.any());
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("should delete admin when SUPER_ADMIN")
        void shouldDeleteAdmin() throws Exception {
            doNothing().when(adminService).delete(ADMIN_ID);

            mockMvc.perform(delete("/api/v1/admins/{id}", ADMIN_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Admin deleted successfully"));

            verify(adminService).delete(ADMIN_ID);
        }
    }
}

