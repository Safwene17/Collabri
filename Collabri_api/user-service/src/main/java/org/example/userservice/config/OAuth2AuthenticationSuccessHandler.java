package org.example.userservice.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    // inject from config
    @Value("${frontend.success.url}")
    private String frontendSuccessUrl;

    // token cookie properties (tweak for prod)
    @Value("${security.cookies.secure:true}")
    private boolean cookieSecure;

    @Value("${security.cookies.http-only:true}")
    private boolean cookieHttpOnly;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = extractEmail(oauthUser.getAttributes());
        String firstname = (String) oauthUser.getAttributes().getOrDefault("given_name",
                oauthUser.getAttributes().getOrDefault("name", "unknown"));

        Optional<User> optionalUser = userRepository.findByEmail(email);
        User user = optionalUser.orElseGet(() -> {
            User u = User.builder()
                    .email(email)
                    .firstname(firstname)
                    .role(Role.USER)
                    .password("") // OAuth user has no local password
                    .build();
            return userRepository.save(u);
        });

        // build a lightweight UserDetails for token generation
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword() == null ? "" : user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails); // consider storing server-side for revocation

        // --- Option A (recommended): set secure HttpOnly cookies instead of query params ---
        // Access token cookie (short-lived)
        Cookie accessCookie = new Cookie("ACCESS_TOKEN", accessToken);
        accessCookie.setHttpOnly(cookieHttpOnly);
        accessCookie.setSecure(cookieSecure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (jwtService.getAccessTokenExpirationSeconds())); // provide getter
        // optionally set SameSite via response header if container does not support setAttribute
        response.addCookie(accessCookie);

        // If you really need refresh token accessible to frontend, set another cookie (HttpOnly)
        Cookie refreshCookie = new Cookie("REFRESH_TOKEN", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) (jwtService.getRefreshTokenExpirationSeconds()));
        response.addCookie(refreshCookie);

        // Redirect to frontend without tokens in URL
        String redirect = UriComponentsBuilder.fromUriString(frontendSuccessUrl).build().toUriString();
        response.sendRedirect(redirect);

    }

    private String extractEmail(Map<String, Object> attributes) {
        if (attributes.containsKey("email") && attributes.get("email") != null) {
            return (String) attributes.get("email");
        }
        // For GitHub, email often not present in /user; `user:email` scope + custom OAuth2UserService is recommended.
        if (attributes.containsKey("login")) {
            return attributes.get("login") + "@github.local";
        }
        return "unknown@example.com";
    }
}
