package org.example.userservice.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.services.CustomUserDetailsService;
import org.example.userservice.services.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * JwtAuthFilter:
 * - reads token from Authorization header (Bearer) OR ACCESS_TOKEN cookie
 * - validates token using JwtService
 * - if valid, loads UserDetails and sets SecurityContext
 */
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    private static final String COOKIE_NAME = "ACCESS_TOKEN";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // parse username safely
                String username = null;
                try {
                    username = jwtService.extractUsername(token);
                } catch (Exception e) {
                    log.debug("JWT parse error: {}", e.toString());
                }

                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(token, userDetails)) {
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        log.debug("Authenticated request for user: {}", username);
                    } else {
                        log.debug("Token invalid or expired for user: {}", username);
                    }
                } else {
                    log.debug("No username extracted from token");
                }
            }
        } catch (Exception ex) {
            // don't break the chain — just log
            log.warn("Error in JwtAuthFilter: {}", ex.toString());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1) Authorization header
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        // 2) ACCESS_TOKEN cookie
        if (request.getCookies() != null) {
            Optional<Cookie> access = Arrays.stream(request.getCookies())
                    .filter(c -> COOKIE_NAME.equals(c.getName()))
                    .findFirst();
            if (access.isPresent()) {
                return access.get().getValue();
            }
        }

        return null;
    }
}
