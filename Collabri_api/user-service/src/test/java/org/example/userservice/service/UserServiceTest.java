package org.example.userservice.service;

import org.example.userservice.dto.UserRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.jwt.RefreshTokenService;
import org.example.userservice.mappers.UserMapper;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;


    private User buildUser(UUID id) {
        return User.builder()
                .firstname("John")
                .lastname("Doe")
                .email("john@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .verified(true)
                .build();
    }

    private UserResponse buildUserResponse(UUID id) {
        return new UserResponse(id, "John", "Doe", "john@example.com");
    }

    private UserRequest fullRequest(String firstname, String lastname, String email, String password) {
        return new UserRequest(firstname, lastname, email, password);
    }

    @Nested
    @DisplayName("createUser()")
    class CreateUser {

        @Test
        @DisplayName("should map, encode password, and save new user when email is available")
        void shouldCreateUserSuccessfully() {
            UserRequest request = fullRequest("John", "Doe", "john@example.com", "RawPass1!");
            User mappedUser = buildUser(null);

            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userMapper.toUser(request)).thenReturn(mappedUser);
            when(passwordEncoder.encode("RawPass1!")).thenReturn("encoded-password");

            userService.createUser(request);

            // Verify password was encoded before save — plaintext must never reach the DB
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
        }

        @Test
        @DisplayName("should encode password onto the mapped entity — not the original request value")
        void shouldSetEncodedPasswordOnMappedEntity() {
            UserRequest request = fullRequest("John", "Doe", "john@example.com", "RawPass1!");
            User mappedUser = buildUser(null); // mapper returns entity with raw password initially

            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userMapper.toUser(request)).thenReturn(mappedUser);
            when(passwordEncoder.encode("RawPass1!")).thenReturn("hashed-value");

            userService.createUser(request);

            // The entity saved must carry the encoded value, not whatever toUser() set
            verify(passwordEncoder).encode("RawPass1!");
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("hashed-value");
        }

        @Test
        @DisplayName("should throw DataIntegrityViolationException when email already exists")
        void shouldThrowWhenEmailAlreadyExists() {
            UserRequest request = fullRequest("John", "Doe", "john@example.com", "RawPass1!");
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessage("Email already exists");

            // mapper and repository.save must never be reached
            verifyNoInteractions(userMapper);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not interact with mapper or save when email is duplicate")
        void shouldAbortEarlyOnDuplicateEmail() {
            UserRequest request = fullRequest("John", "Doe", "duplicate@example.com", "RawPass1!");
            when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessage("Email already exists");

            verifyNoInteractions(userMapper);
            verifyNoInteractions(passwordEncoder);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("should revoke all tokens then delete user when found")
        void shouldRevokeTokensThenDeleteUser() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            userService.delete(id);

            // Verify order: revoke THEN delete — FK integrity depends on this
            var inOrder = inOrder(refreshTokenService, userRepository);
            inOrder.verify(refreshTokenService).revokeAllTokensForUser(user);
            inOrder.verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when user does not exist")
        void shouldThrowNotFoundWhenUserMissing() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.delete(id))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("User not found");

            // Neither token revocation nor deletion should be attempted
            verifyNoInteractions(refreshTokenService);
            verify(userRepository, never()).delete(any());
        }
    }



    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("should return mapped UserResponse when user exists")
        void shouldReturnMappedResponseWhenUserExists() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id);
            UserResponse expected = buildUserResponse(id);

            when(userRepository.findById(id)).thenReturn(Optional.of(user));
            when(userMapper.fromUser(user)).thenReturn(expected);

            UserResponse result = userService.findById(id);

            assertThat(result).isEqualTo(expected);
            verify(userMapper).fromUser(user);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when user does not exist")
        void shouldThrowNotFoundWhenUserMissing() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(id))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("User not found");

            verifyNoInteractions(userMapper);
        }
    }


    @Nested
    @DisplayName("findAllPaginated()")
    class FindAllPaginated {

        @Test
        @DisplayName("should return page of mapped UserResponse")
        void shouldReturnPageOfMappedResponses() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id);
            UserResponse response = buildUserResponse(id);
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

            when(userRepository.findAll(pageable)).thenReturn(userPage);
            when(userMapper.fromUser(user)).thenReturn(response);

            Page<UserResponse> result = userService.findAllPaginated(pageable);

            assertThat(result.getContent()).containsExactly(response);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty page when no users exist")
        void shouldReturnEmptyPageWhenNoUsers() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

            Page<UserResponse> result = userService.findAllPaginated(pageable);

            assertThat(result.getContent()).isEmpty();
            verifyNoInteractions(userMapper); // mapper must not be called on empty page
        }
    }


    @Nested
    @DisplayName("findByEmail()")
    class FindByEmail {

        @Test
        @DisplayName("should return mapped UserResponse when email exists")
        void shouldReturnMappedResponseWhenEmailExists() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id);
            UserResponse expected = buildUserResponse(id);

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
            when(userMapper.fromUser(user)).thenReturn(expected);

            UserResponse result = userService.findByEmail("john@example.com");

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when email does not exist")
        void shouldThrowNotFoundWhenEmailMissing() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByEmail("ghost@example.com"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("User not found");
        }
    }


    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("should throw NOT_FOUND when user does not exist")
        void shouldThrowNotFoundWhenUserMissing() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.update(id,
                    fullRequest("Jane", "Smith", "jane@example.com", "NewPass1!")))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("User not found");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should update firstname and lastname when valid")
        void shouldUpdateFirstnameAndLastname() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            userService.update(id, fullRequest("Jane", "Smith", "john@example.com", "NewPass1!"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();

            assertThat(saved.getFirstname()).isEqualTo("Jane");
            assertThat(saved.getLastname()).isEqualTo("Smith");
        }

        @Test
        @DisplayName("should update email when new email is different and not taken")
        void shouldUpdateEmailWhenDifferentAndAvailable() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id); // current email: john@example.com
            when(userRepository.findById(id)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);

            userService.update(id, fullRequest("John", "Doe", "newemail@example.com", "NewPass1!"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            assertThat(captor.getValue().getEmail()).isEqualTo("newemail@example.com");
        }

        @Test
        @DisplayName("should throw UNPROCESSABLE_ENTITY when new email is already taken")
        void shouldThrowWhenEmailAlreadyTaken() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id); // current: john@example.com
            when(userRepository.findById(id)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.update(id,
                    fullRequest("John", "Doe", "taken@example.com", "NewPass1!")))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessage("Email already in use");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should NOT check email availability when new email equals current email")
        void shouldNotCheckEmailAvailabilityWhenEmailUnchanged() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id); // current: john@example.com
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            // same email as current — existsByEmail must never be called
            userService.update(id, fullRequest("John", "Doe", "john@example.com", "NewPass1!"));

            verify(userRepository, never()).existsByEmail(any());
            verify(userRepository).save(any());
        }

        @Test
        @DisplayName("should encode and update password when provided")
        void shouldEncodeAndUpdatePassword() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NewPass1!")).thenReturn("hashed-new-password");

            userService.update(id, fullRequest("John", "Doe", "john@example.com", "NewPass1!"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            verify(passwordEncoder).encode("NewPass1!");

            assertThat(captor.getValue().getPassword()).isEqualTo("hashed-new-password");
        }

        @Test
        @DisplayName("should NOT update password when password field is null")
        void shouldNotUpdatePasswordWhenNull() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            // UserRequest with null password — simulates partial update intent
            userService.update(id, new UserRequest("John", "Doe", "john@example.com", null));

            verifyNoInteractions(passwordEncoder);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password"); // unchanged
        }

        @Test
        @DisplayName("should NOT update password when password field is blank")
        void shouldNotUpdatePasswordWhenBlank() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            userService.update(id, new UserRequest("John", "Doe", "john@example.com", "   "));

            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("should always save user after any valid update")
        void shouldAlwaysSaveAfterUpdate() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            userService.update(id, fullRequest("John", "Doe", "john@example.com", "NewPass1!"));

            verify(userRepository, times(1)).save(user);
        }
    }
}