package org.example.notificationservice.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom converter to extract authorities from JWT "roles" claim
 * and set the principal name to userId (UUID string).
 */
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String userId = jwt.getClaimAsString("userId");

        // Use userId as principal name if available, otherwise use "sub" (email)
        String principalName = userId != null ? userId : jwt.getSubject();

        return new UsernamePasswordAuthenticationToken(
                principalName,
                jwt.getTokenValue(),
                authorities
        );
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");

        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        return roles.stream()
                .map(role -> {
                    // Add "ROLE_" prefix if not already present
                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    return new SimpleGrantedAuthority(authority);
                })
                .collect(Collectors.toList());
    }
}

