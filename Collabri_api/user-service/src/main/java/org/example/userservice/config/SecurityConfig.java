package org.example.userservice.config;


import org.example.userservice.filters.JwtAuthFilter;
import org.example.userservice.services.CustomUserDetailsService;
import org.example.userservice.services.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            OAuth2AuthenticationSuccessHandler successHandler
    ) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/users/resend-verification",
                                "/api/v1/users/verify-email",
                                "/api/v1/users/get/**",
                                "/api/v1/users/register",
                                "/api/v1/users/forgot-password",
                                "/api/v1/users/reset-password",
                                "/api/v1/users/login",
                                "/oauth2/**",                // allow oauth endpoints
                                "/login/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/api/v1/users/delete/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/update/**").hasAnyRole("ADMIN", "USER")

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler))
                // Simple approach: just return HTTP status codes
                .exceptionHandling(exceptions -> exceptions
                        // For API endpoints, return 401 status instead of redirecting
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        // For access denied, return 403 status
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.setStatus(HttpStatus.FORBIDDEN.value()))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        ;
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService, CustomUserDetailsService customUserDetailsService) {
        return new JwtAuthFilter(jwtService, customUserDetailsService);
    }
}
