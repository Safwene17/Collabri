// src/main/java/org/example/gateway/filters/JwtValidationFilter.java
package org.example.gateway.filters;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.dto.ApiResponse;
import org.example.gateway.util.ApiResponseWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtValidationFilter implements Filter {

    private final JwtDecoder jwtDecoder;
    private final ApiResponseWriter apiResponseWriter;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // No token → public endpoint → continue
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No JWT — public endpoint: {}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            jwtDecoder.decode(token);
            log.debug("Valid JWT — proceeding to microservice: {}", request.getRequestURI());
            chain.doFilter(request, response); // ← VALID TOKEN → CONTINUE

        } catch (ExpiredJwtException e) {
            log.info("Expired JWT from {}: {}", request.getRemoteAddr(), e.getMessage());
            apiResponseWriter.write(response, HttpStatus.UNAUTHORIZED.value(),
                    ApiResponse.error("Token has expired"));

        } catch (JwtValidationException | JwtException e) {
            log.warn("Invalid JWT from {}: {}", request.getRemoteAddr(), e.getMessage());
            apiResponseWriter.write(response, HttpStatus.UNAUTHORIZED.value(),
                    ApiResponse.error("Invalid token"));
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation", e);
            apiResponseWriter.write(response, HttpStatus.UNAUTHORIZED.value(),
                    ApiResponse.error("Missing authentication token"));
        }
    }
}