package org.example.userservice.entities;

import org.example.userservice.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("User Entity Unit Tests")
class UserTest {

    @Nested
    @DisplayName("UserDetails tests")
    class UserDetailsContract {

        @Test
        @DisplayName("should use email as username")
        void shouldUseEmailAsUsername() {
            User user = User.builder()
                    .email("john.doe@example.com")
                    .build();

            assertThat(user.getUsername()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("should return encoded password")
        void shouldReturnEncodedPassword() {
            User user = User.builder().password("encodedSecret123").build();

            assertThat(user.getPassword()).isEqualTo("encodedSecret123");
        }

        @ParameterizedTest
        @EnumSource(Role.class)
        @DisplayName("should return authority with ROLE_ prefix")
        void shouldReturnAuthorityWithRolePrefix(Role role) {
            User user = User.builder().role(role).build();

            var authorities = user.getAuthorities();

            assertThat(authorities)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(SimpleGrantedAuthority.class)
                    .extracting("authority")
                    .isEqualTo("ROLE_" + role.name());
        }

        @Test
        @DisplayName("should return true for all default UserDetails checks")
        void shouldReturnTrueForDefaultSecurityChecks() {
            User user = User.builder().build();

            assertThat(user.isAccountNonExpired()).isTrue();
            assertThat(user.isAccountNonLocked()).isTrue();
            assertThat(user.isCredentialsNonExpired()).isTrue();
            assertThat(user.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Builder and Default Values")
    class BuilderAndDefaults {

        @Test
        @DisplayName("should set default role to USER")
        void shouldSetDefaultRoleToUser() {
            User user = User.builder()
                    .email("test@example.com")
                    .build();

            assertThat(user.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("should set default verified to false")
        void shouldSetDefaultVerifiedToFalse() {
            User user = User.builder()
                    .email("test@example.com")
                    .build();

            assertThat(user.isVerified()).isFalse();
        }

        @Test
        @DisplayName("should create user correctly using builder")
        void shouldCreateUserWithBuilder() {
            User user = User.builder()
                    .firstname("John")
                    .lastname("Doe")
                    .email("john.doe@example.com")
                    .password("pass123")
                    .role(Role.ADMIN)
                    .verified(true)
                    .build();

            assertThat(user.getFirstname()).isEqualTo("John");
            assertThat(user.getLastname()).isEqualTo("Doe");
            assertThat(user.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(user.getRole()).isEqualTo(Role.ADMIN);
            assertThat(user.isVerified()).isTrue();
        }
    }

    @Nested
    @DisplayName("Role Management")
    class RoleManagement {

        @Test
        @DisplayName("should update role and authorities correctly")
        void shouldUpdateRoleAndAuthorities() {
            User user = User.builder()
                    .role(Role.USER)
                    .build();

            user.setRole(Role.ADMIN);

            assertThat(user.getRole()).isEqualTo(Role.ADMIN);
            assertThat(user.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ADMIN");
        }
    }
}