// file: src/main/java/org/example/calendarservice/config/VerifiedUserChecker.java
package org.example.calendarservice.config;

import org.example.calendarservice.exceptions.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("verified")
public class VerifiedUserChecker {

    public boolean isVerified(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtToken)) {
            throw new CustomException("Invalid authentication token", HttpStatus.UNAUTHORIZED);
        }

        // ADDED: Bypass verification for admins
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))) {
            return true;
        }

        Jwt jwt = jwtToken.getToken();
        Boolean verified = jwt.getClaimAsBoolean("verified");

        if (!Boolean.TRUE.equals(verified)) {
            throw new CustomException("Email not verified", HttpStatus.FORBIDDEN);
        }
        return true;
    }
}