package org.example.userservice.security;

import lombok.RequiredArgsConstructor;
import org.example.userservice.entities.User;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.CustomUserDetailsService;
import org.example.userservice.services.TokenService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;

    // This is the ONLY redirect the browser will ever see after Google
    private static final String FRONTEND_URL = "http://localhost:5173/home";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        DefaultOAuth2User oauthUser = (DefaultOAuth2User) authentication.getPrincipal();
        String email = (String) oauthUser.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // This method must set HttpOnly Secure SameSite=None cookies
        // (or SameSite=Lax if you don't need credentials on cross-site)
        tokenService.issueTokens(user, userDetails, response);

        // ONE single redirect to the frontend
        response.sendRedirect(FRONTEND_URL);
    }
}

