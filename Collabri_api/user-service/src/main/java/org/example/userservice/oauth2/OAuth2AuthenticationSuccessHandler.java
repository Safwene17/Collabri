package org.example.userservice.oauth2;

import lombok.RequiredArgsConstructor;
import org.example.userservice.entities.User;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.CustomUserDetailsService;
import org.example.userservice.services.TokenService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;


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

        String accessToken = tokenService.issueTokens(user, userDetails, response);


        String targetUrl = UriComponentsBuilder.fromUriString(FRONTEND_URL)
                .fragment("accessToken=" + accessToken)
                .build()
                .toUriString();

        // Clear any leftover auth attributes and redirect
        new DefaultRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}