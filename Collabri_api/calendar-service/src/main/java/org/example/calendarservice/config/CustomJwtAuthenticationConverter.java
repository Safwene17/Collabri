// file: src/main/java/org/example/calendarservice/config/CustomJwtAuthenticationConverter.java
package org.example.calendarservice.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.core.convert.converter.Converter;

import java.util.Collection;

public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");  // CHANGED: Remove prefix since JWT roles already include "ROLE_"

        Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);

        String principalId = jwt.getClaimAsString("userId");
        if (principalId == null) {
            principalId = jwt.getClaimAsString("adminId");  // ADDED: Fallback to adminId for admins
        }

        return new JwtAuthenticationToken(jwt, authorities, principalId);  // Principal name now handles both userId and adminId
    }
}