package org.example.userservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.CustomUserDetailsService;
import org.example.userservice.services.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final CustomUserDetailsService userDetailsService;

    @Value("${frontend.success.url}")
    private String frontendSuccessUrl;

    @Value("${security.cookies.secure:false}")
    private boolean cookieSecure;

    @Value("${security.cookies.http-only:true}")
    private boolean cookieHttpOnly;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        try {
            String email = safeString(oauthUser.getAttributes().get("email"));
            String givenName = safeString(oauthUser.getAttributes().get("given_name"));
            String familyName = safeString(oauthUser.getAttributes().get("family_name"));

            if ((givenName == null || givenName.isBlank()) && oauthUser.getAttributes().get("name") != null) {
                String full = oauthUser.getAttributes().get("name").toString();
                String[] parts = splitName(full);
                givenName = parts[0];
                familyName = parts[1];
            }

            // Find or create user
            String finalGivenName = givenName;
            String finalFamilyName = familyName;
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = User.builder()
                        .email(email)
                        .firstname(finalGivenName)
                        .lastname(finalFamilyName)
                        .role(Role.USER)
                        .password("") // OAuth user has no password
                        .verified(true)
                        .build();
                return userRepository.save(newUser);
            });

            // Load UserDetails
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

            // Issue tokens via centralized TokenService
            tokenService.issueTokens(user, userDetails, response, cookieSecure, cookieHttpOnly);

            response.sendRedirect(frontendSuccessUrl);
        } catch (Exception ex) {
            log.error("OAuth2 login error", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth processing failed");
        }
    }

    private static String safeString(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"", ""};
        String[] parts = fullName.trim().split("\\s+", 2);
        return new String[]{parts[0], parts.length > 1 ? parts[1] : ""};
    }
}
