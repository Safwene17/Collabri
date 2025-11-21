package org.example.gateway.config;

import lombok.experimental.UtilityClass;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@UtilityClass
public class ApiWhitelist {

    private final AntPathMatcher MATCHER = new AntPathMatcher();

    public final List<String> ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/validate-password-reset-token",
            "/api/v1/auth/validate-email-verification-token",
            "/oauth2/**",
            "/login/oauth2/**",
            "/error"
    );

    public boolean isWhitelisted(String path) {
        return ENDPOINTS.stream().anyMatch(pattern -> MATCHER.match(pattern, path));
    }
}
