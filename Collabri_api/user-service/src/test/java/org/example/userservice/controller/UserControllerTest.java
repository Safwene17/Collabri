package org.example.userservice.controller;

import org.example.userservice.config.SecurityConfigTest;
import org.example.userservice.controllers.UserController;
import org.example.userservice.dto.UserRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.services.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfigTest.class)
@DisplayName("UserController Web Layer Tests")
//@AutoConfigureMockMvc(addFilters = false) //skip all sort of security filters
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @AfterEach
    void resetMocks() {
        reset(userService);
    }

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "john.doe@example.com";

    private UserResponse buildUserResponse() {
        return new UserResponse(USER_ID, "John", "Doe", EMAIL);
    }


    private String validUserJson() {
        return """
                {
                    "firstname": "John",
                    "lastname": "Doe",
                    "email": "john.doe@example.com",
                    "password": "StrongPass1!"
                }
                """;
    }


    @Nested
    @DisplayName("POST /api/v1/users — createUser")
    class CreateUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 200 and success message when ADMIN creates user")
        void shouldCreateUserAsAdmin() throws Exception {
            doNothing().when(userService).createUser(any(UserRequest.class));

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User created successfully"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(userService).createUser(any(UserRequest.class));
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("should return 200 when SUPER_ADMIN creates user")
        void shouldCreateUserAsSuperAdmin() throws Exception {
            doNothing().when(userService).createUser(any(UserRequest.class));

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userService).createUser(any(UserRequest.class));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 403 when USER role attempts to create user")
        void shouldReturnForbiddenForUserRole() throws Exception {
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("should return 401 when request is unauthenticated")
        void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 400 when firstname is blank")
        void shouldReturnBadRequestWhenFirstnameBlank() throws Exception {
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstname": "",
                                        "lastname": "Doe",
                                        "email": "john@example.com",
                                        "password": "StrongPass1!"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 400 when password fails complexity rules")
        void shouldReturnBadRequestWhenPasswordTooWeak() throws Exception {
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstname": "John",
                                        "lastname": "Doe",
                                        "email": "john@example.com",
                                        "password": "weakpass"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 400 when email format is invalid")
        void shouldReturnBadRequestWhenEmailInvalid() throws Exception {
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstname": "John",
                                        "lastname": "Doe",
                                        "email": "not-an-email",
                                        "password": "StrongPass1!"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id} — getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return 200 with user body — no authentication required")
        void shouldReturnUserById() throws Exception {
            when(userService.findById(USER_ID)).thenReturn(buildUserResponse());

            mockMvc.perform(get("/api/v1/users/{id}", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.firstname").value("John"))
                    .andExpect(jsonPath("$.lastname").value("Doe"));

            verify(userService).findById(USER_ID);
        }

        @Test
        @DisplayName("should return 404 when user does not exist")
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(userService.findById(USER_ID))
                    .thenThrow(new CustomException("User not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/v1/users/{id}", USER_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/by-email — getUserByEmail")
    class GetUserByEmail {

        @Test
        @DisplayName("should return 200 with user wrapped in ApiResponse — no auth required")
        void shouldReturnUserByEmail() throws Exception {
            when(userService.findByEmail(EMAIL)).thenReturn(buildUserResponse());

            mockMvc.perform(get("/api/v1/users/by-email")
                            .param("email", EMAIL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User fetched successfully"))
                    .andExpect(jsonPath("$.data.email").value(EMAIL))
                    .andExpect(jsonPath("$.data.firstname").value("John"));

            verify(userService).findByEmail(EMAIL);
        }

        @Test
        @DisplayName("should return 404 when no user has that email")
        void shouldReturn404WhenEmailNotFound() throws Exception {
            when(userService.findByEmail("ghost@example.com"))
                    .thenThrow(new CustomException("User not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/v1/users/by-email")
                            .param("email", "ghost@example.com"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when email query param is missing")
        void shouldReturn400WhenEmailParamMissing() throws Exception {
            mockMvc.perform(get("/api/v1/users/by-email"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }
    }


    @Nested
    @DisplayName("GET /api/v1/users — getAllUsers (paginated)")
    class GetAllUsers {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return paginated users when ADMIN")
        void shouldReturnPaginatedUsersAsAdmin() throws Exception {
            Page<UserResponse> page = new PageImpl<>(List.of(buildUserResponse()));
            when(userService.findAllPaginated(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/users")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Users fetched successfully"))
                    .andExpect(jsonPath("$.data.content[0].email").value(EMAIL))
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            verify(userService).findAllPaginated(any(Pageable.class));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 403 when USER role tries to list all users")
        void shouldReturnForbiddenForUserRole() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(userService);
        }
    }


    @Nested
    @DisplayName("PUT /api/v1/users/{id} — updateUser")
    class UpdateUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 202 when ADMIN updates any user")
        void shouldUpdateUserAsAdmin() throws Exception {
            doNothing().when(userService).update(eq(USER_ID), any(UserRequest.class));

            mockMvc.perform(put("/api/v1/users/{id}", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User updated successfully"));

            verify(userService).update(eq(USER_ID), any(UserRequest.class));
        }

        @Test
        @WithMockUser(username = "c0f7e8d9-1234-5678-9abc-def012345678", roles = "USER")
        @DisplayName("should return 202 when USER updates their own profile — self-update")
        void shouldAllowSelfUpdate() throws Exception {
            UUID selfId = UUID.fromString("c0f7e8d9-1234-5678-9abc-def012345678");
            doNothing().when(userService).update(eq(selfId), any(UserRequest.class));

            mockMvc.perform(put("/api/v1/users/{id}", selfId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userService).update(eq(selfId), any(UserRequest.class));
        }

        @Test
        @WithMockUser(username = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", roles = "USER")
        @DisplayName("should return 403 when USER tries to update another user's profile")
        void shouldReturnForbiddenWhenUpdatingOtherUser() throws Exception {
            UUID otherId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

            mockMvc.perform(put("/api/v1/users/{id}", otherId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(put("/api/v1/users/{id}", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 400 when request body fails validation")
        void shouldReturnBadRequestForInvalidBody() throws Exception {
            mockMvc.perform(put("/api/v1/users/{id}", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstname": "",
                                        "lastname": "",
                                        "email": "not-valid",
                                        "password": "weak"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }
    }


    @Nested
    @DisplayName("DELETE /api/v1/users/{id} — deleteUser")
    class DeleteUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 200 when ADMIN deletes user")
        void shouldDeleteUserAsAdmin() throws Exception {
            doNothing().when(userService).delete(USER_ID);

            mockMvc.perform(delete("/api/v1/users/{id}", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User deleted successfully"));

            verify(userService).delete(USER_ID);
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("should return 200 when SUPER_ADMIN deletes user")
        void shouldDeleteUserAsSuperAdmin() throws Exception {
            doNothing().when(userService).delete(USER_ID);

            mockMvc.perform(delete("/api/v1/users/{id}", USER_ID))
                    .andExpect(status().isOk());

            verify(userService).delete(USER_ID);
        }

        @Test
        @WithMockUser(username = "c0f7e8d9-1234-5678-9abc-def012345678", roles = "USER")
        @DisplayName("should return 403 for USER role — HTTP layer fires before @PreAuthorize self-delete")
        void shouldReturnForbiddenForUserEvenOnSelfDelete() throws Exception {
            UUID selfId = UUID.fromString("c0f7e8d9-1234-5678-9abc-def012345678");

            mockMvc.perform(delete("/api/v1/users/{id}", selfId))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(delete("/api/v1/users/{id}", USER_ID))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 404 when service throws NOT_FOUND")
        void shouldReturn404WhenUserNotFound() throws Exception {
            doThrow(new CustomException("User not found", HttpStatus.NOT_FOUND))
                    .when(userService).delete(USER_ID);

            mockMvc.perform(delete("/api/v1/users/{id}", USER_ID))
                    .andExpect(status().isNotFound());
        }
    }
}

