package org.example.userservice.service;

import org.example.userservice.dto.UserRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.jwt.RefreshTokenService;
import org.example.userservice.mappers.UserMapper;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    private User buildAdmin(UUID id, String email) {
        return User.builder()
                .id(id)
                .firstname("Admin")
                .lastname("User")
                .email(email)
                .password("encoded")
                .role(Role.ADMIN)
                .verified(true)
                .build();
    }

    private UserRequest buildRequest(String email, String password) {
        return new UserRequest("Alice", "Admin", email, password);
    }

    // Protects create-admin business rules and role assignment.
    @Nested
    @DisplayName("createAdmin()")
    class CreateAdmin {

        @Test
        @DisplayName("should assign ADMIN role, encode password, and save")
        void shouldCreateAdminSuccessfully() {
            UserRequest request = buildRequest("admin@example.com", "StrongPass1!");
            User mapped = User.builder().email("admin@example.com").password("raw").build();

            when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
            when(userMapper.toUser(request)).thenReturn(mapped);
            when(passwordEncoder.encode("raw")).thenReturn("hashed");

            adminService.createAdmin(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
            assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        }

        @Test
        @DisplayName("should throw 422 when email already exists")
        void shouldThrowWhenEmailExists() {
            UserRequest request = buildRequest("dup-admin@example.com", "StrongPass1!");
            when(userRepository.existsByEmail("dup-admin@example.com")).thenReturn(true);

            assertThatThrownBy(() -> adminService.createAdmin(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Email already exists")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

            verifyNoInteractions(userMapper, passwordEncoder);
        }
    }

    // Protects retrieval mapping for admin listing.
    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("should map all entities to responses")
        void shouldMapAll() {
            User user = buildAdmin(UUID.randomUUID(), "a@example.com");
            PageRequest pageable = PageRequest.of(0, 10);
            Page<User> users = new PageImpl<>(List.of(user), pageable, 1);
            UserResponse response = new UserResponse(user.getId(), "Admin", "User", "a@example.com");

            when(userRepository.findAll(pageable)).thenReturn(users);
            when(userMapper.fromUser(user)).thenReturn(response);

            Page<UserResponse> result = adminService.findAll(pageable);

            assertThat(result.getContent()).containsExactly(response);
        }
    }

    // Protects not-found path and mapping behavior for admin retrieval by id.
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("should return mapped admin when found")
        void shouldReturnMappedAdmin() {
            UUID id = UUID.randomUUID();
            User admin = buildAdmin(id, "a@example.com");
            UserResponse response = new UserResponse(id, "Admin", "User", "a@example.com");

            when(userRepository.findById(id)).thenReturn(Optional.of(admin));
            when(userMapper.fromUser(admin)).thenReturn(response);

            UserResponse result = adminService.findById(id);

            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("should throw 404 when admin not found")
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.findById(id))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Admin not found")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // Protects update validation, conditional field updates, and duplicate-email rejection.
    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("should update basic fields and save")
        void shouldUpdateBasicFields() {
            UUID id = UUID.randomUUID();
            User admin = buildAdmin(id, "old@example.com");
            UserRequest request = new UserRequest("New", "Name", "new@example.com", "StrongPass1!");

            when(userRepository.findById(id)).thenReturn(Optional.of(admin));
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("StrongPass1!")).thenReturn("new-hash");

            adminService.update(id, request);

            assertThat(admin.getFirstname()).isEqualTo("New");
            assertThat(admin.getLastname()).isEqualTo("Name");
            assertThat(admin.getEmail()).isEqualTo("new@example.com");
            assertThat(admin.getPassword()).isEqualTo("new-hash");
            verify(userRepository).save(admin);
        }

        @Test
        @DisplayName("should throw 422 when new email exists")
        void shouldThrowWhenNewEmailTaken() {
            UUID id = UUID.randomUUID();
            User admin = buildAdmin(id, "old@example.com");
            UserRequest request = new UserRequest("New", "Name", "taken@example.com", "StrongPass1!");

            when(userRepository.findById(id)).thenReturn(Optional.of(admin));
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThatThrownBy(() -> adminService.update(id, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Email already exists")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    // Protects delete flow order: revoke sessions before removing admin entity.
    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("should revoke all tokens then delete admin")
        void shouldRevokeThenDelete() {
            UUID id = UUID.randomUUID();
            User admin = buildAdmin(id, "admin@example.com");
            when(userRepository.findById(id)).thenReturn(Optional.of(admin));

            adminService.delete(id);

            var order = inOrder(refreshTokenService, userRepository);
            order.verify(refreshTokenService).revokeAllTokensForUser(admin);
            order.verify(userRepository).delete(admin);
        }

        @Test
        @DisplayName("should throw 404 when admin to delete is missing")
        void shouldThrowWhenDeleteTargetMissing() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.delete(id))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Admin not found")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verifyNoInteractions(refreshTokenService);
        }
    }
}

