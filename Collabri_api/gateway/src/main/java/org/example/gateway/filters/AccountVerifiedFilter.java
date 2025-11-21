package org.example.gateway.filters;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.dto.ApiResponse;
import org.example.gateway.config.ApiWhitelist;
import org.example.gateway.util.JwtUtils;
import org.example.gateway.util.ApiResponseWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountVerifiedFilter implements Filter {

    private final JwtUtils jwtUtils;
    private final ApiResponseWriter apiResponseWriter;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // 1. Skip preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Skip whitelist
        String path = request.getRequestURI();
        if (ApiWhitelist.isWhitelisted(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 3. Read Authorization header
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            apiResponseWriter.write(response, 401, ApiResponse.error("Missing or invalid Authorization token"));
            return;
        }

        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            apiResponseWriter.write(response, 401, ApiResponse.error("Missing or invalid Authorization token"));
            return;
        }

        // 4. Parse JWT
        try {
            Claims claims = jwtUtils.parseClaims(token);

            Boolean verified = claims.get("verified", Boolean.class);
            if (verified == null || !verified) {
                log.warn("Blocked request (unverified): {}", path);
                apiResponseWriter.write(response, 403, ApiResponse.error("Email not verified"));
                return;
            }

            chain.doFilter(request, response);

        } catch (Exception ex) {
            log.warn("Invalid JWT at {}: {}", path, ex.getMessage());
            apiResponseWriter.write(response, 401, ApiResponse.error("Invalid token"));
        }
    }
}
