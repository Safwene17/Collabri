package org.example.userservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    // inject from config
    @Value("${frontend.success.url}")
    private String frontendSuccessUrl;

    // token cookie properties (tweak for prod)
    @Value("${security.cookies.secure:false}")
    private boolean cookieSecure;

    @Value("${security.cookies.http-only:true}")
    private boolean cookieHttpOnly;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        try {
//            String email = extractEmail(oauthUser);
            String email = safeString(oauthUser.getAttributes().get("email"));
            String givenName = safeString(oauthUser.getAttributes().get("given_name"));
            String familyName = safeString(oauthUser.getAttributes().get("family_name"));

            // fallback to generic "name" (GitHub often provides a full name in "name")
            if (givenName == null || givenName.isBlank()) {
                String full = safeString(oauthUser.getAttributes().get("name"));
                if (full != null) {
                    String[] parts = splitName(full);
                    givenName = parts.length > 0 ? parts[0] : null;
                    familyName = parts.length > 1 ? parts[1] : null;
                }
            }


            // find or create user; for creation we populate available fields (do not invent meaningful defaults)
            Optional<User> optionalUser = userRepository.findByEmail(email);
            User user;
            if (optionalUser.isPresent()) {
                user = optionalUser.get();
            } else {
                user = User.builder()
                        .email(email)
                        .firstname(givenName)
                        .lastname(familyName)
                        .role(Role.USER)
                        .password("") // OAuth user has no local password by default
                        .enabled(true)
                        .build();
                userRepository.save(user);
            }


            // build a lightweight UserDetails for token generation
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPassword())
                    .authorities("ROLE_" + user.getRole().name())
                    .build();

            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails); // consider storing server-side for revocation

            ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", accessToken)
                    .httpOnly(cookieHttpOnly)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(jwtService.getAccessTokenExpirationSeconds())
                    .build();

            ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(jwtService.getRefreshTokenExpirationSeconds())
                    .build();

            // add cookies via Set-Cookie headers
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // redirect to frontend (no tokens in URL)
            String redirect = UriComponentsBuilder.fromUriString(frontendSuccessUrl).build().toUriString();
            response.sendRedirect(redirect);

        } catch (Exception ex) {
            // don't leak internal details to the user; log and redirect to error page if you have one
            log.error("Error handling OAuth2 success", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth processing failed");
        }
    }


    private static String safeString(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * Splits a full name into [first, restJoined]
     * Example: "Jean Claude Van Damme" -> ["Jean", "Claude Van Damme"]
     */
    private static String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[0];
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return new String[]{parts[0], ""};
        String first = parts[0];
        String rest = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
        return new String[]{first, rest};
    }
}