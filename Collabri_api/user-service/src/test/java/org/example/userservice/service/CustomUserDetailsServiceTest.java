package org.example.userservice.service;

import org.example.userservice.entities.User;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User buildUser(String email) {
        return User.builder()
                .email(email)
                .password("encoded")
                .firstname("John")
                .lastname("Doe")
                .build();
    }

    // Protects authentication principal loading by email.
    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("should return user details when email exists")
        void shouldReturnUserDetailsWhenFound() {
            User user = buildUser("existing@example.com");
            when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(user));

            var details = customUserDetailsService.loadUserByUsername("existing@example.com");

            assertThat(details).isSameAs(user);
            assertThat(details.getUsername()).isEqualTo("existing@example.com");
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when email does not exist")
        void shouldThrowWhenUserMissing() {
            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found with email: missing@example.com");
        }
    }
}

