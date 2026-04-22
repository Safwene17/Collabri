package org.example.userservice.security;

import org.example.userservice.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigCoverageTest.ProbeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=12345678901234567890123456789012")
@DisplayName("SecurityConfig Coverage Tests")
class SecurityConfigCoverageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private org.springframework.core.convert.converter.Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    // Protects route-level access rules configured in SecurityFilterChain.
    @Nested
    @DisplayName("route authorization")
    class RouteAuthorization {

        @Test
        @DisplayName("should return 401 for unauthenticated protected route")
        void shouldReturnUnauthorizedForProtectedRoute() throws Exception {
            mockMvc.perform(get("/internal/secured"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should allow unauthenticated GET by-email route")
        void shouldAllowPublicGetByEmail() throws Exception {
            mockMvc.perform(get("/api/v1/users/by-email"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should allow unauthenticated GET user by id route")
        void shouldAllowPublicGetById() throws Exception {
            mockMvc.perform(get("/api/v1/users/11111111-1111-1111-1111-111111111111"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should deny DELETE for authenticated USER role")
        void shouldDenyDeleteForUserRole() throws Exception {
            mockMvc.perform(delete("/api/v1/users/11111111-1111-1111-1111-111111111111")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.claim("roles", java.util.List.of("ROLE_USER")))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should deny DELETE when JWT has no roles claim")
        void shouldDenyDeleteWhenRolesMissing() throws Exception {
            mockMvc.perform(delete("/api/v1/users/11111111-1111-1111-1111-111111111111")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should allow DELETE for ADMIN role")
        void shouldAllowDeleteForAdminRole() throws Exception {
            mockMvc.perform(delete("/api/v1/users/11111111-1111-1111-1111-111111111111")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should allow PUT for USER role")
        void shouldAllowPutForUserRole() throws Exception {
            mockMvc.perform(put("/api/v1/users/11111111-1111-1111-1111-111111111111")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                    .andExpect(status().isOk());
        }
    }

    // Protects JWT roles claim conversion used by resource-server authentication.
    @Nested
    @DisplayName("jwt authority conversion")
    class JwtAuthorityConversion {

        @Test
        @DisplayName("should convert roles claim into granted authorities")
        void shouldConvertRolesClaim() {
            Jwt jwt = new Jwt(
                    "token",
                    Instant.now(),
                    Instant.now().plusSeconds(300),
                    Map.of("alg", "none"),
                    Map.of("roles", List.of("ROLE_ADMIN"))
            );

            AbstractAuthenticationToken auth = jwtAuthenticationConverter.convert(jwt);

            assertThat(auth).isNotNull();
            assertThat(auth.getAuthorities())
                    .extracting(Object::toString)
                    .contains("ROLE_ADMIN");
        }

        @Test
        @DisplayName("should return empty authorities when roles claim is missing")
        void shouldReturnEmptyAuthoritiesWhenRolesMissing() {
            Jwt jwt = new Jwt(
                    "token",
                    Instant.now(),
                    Instant.now().plusSeconds(300),
                    Map.of("alg", "none"),
                    Map.of("sub", "user-1")
            );

            AbstractAuthenticationToken auth = jwtAuthenticationConverter.convert(jwt);

            assertThat(auth).isNotNull();
            assertThat(auth.getAuthorities()).isEmpty();
        }
    }

    @RestController
    @RequestMapping
    static class ProbeController {

        @GetMapping("/internal/secured")
        ResponseEntity<Void> secured() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/v1/users/by-email")
        ResponseEntity<Void> byEmail() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/v1/users/{id}")
        ResponseEntity<Void> byId(@PathVariable String id) {
            return ResponseEntity.ok().build();
        }

        @DeleteMapping("/api/v1/users/{id}")
        ResponseEntity<Void> deleteUser(@PathVariable String id) {
            return ResponseEntity.ok().build();
        }

        @PutMapping("/api/v1/users/{id}")
        ResponseEntity<Void> updateUser(@PathVariable String id) {
            return ResponseEntity.ok().build();
        }
    }
}




