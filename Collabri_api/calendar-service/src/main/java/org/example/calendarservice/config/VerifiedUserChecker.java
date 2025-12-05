package org.example.calendarservice.config;

import org.example.calendarservice.exceptions.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("verified")
public class VerifiedUserChecker {

    public boolean isVerified(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtToken)) {
            throw new CustomException("Invalid authentication token", HttpStatus.UNAUTHORIZED);
        }

        Jwt jwt = jwtToken.getToken();
        Boolean verified = jwt.getClaimAsBoolean("verified");

        if (!Boolean.TRUE.equals(verified)) {
            throw new CustomException("Email not verified", HttpStatus.FORBIDDEN);
        }
        return true;
    }
}