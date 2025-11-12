package org.example.userservice.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.example.userservice.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private Key signingKey;

    @PostConstruct
    public void init() {
        // ensure secret is long enough. In prod use a proper key / JWKS.
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // generate access token
    public String generateAccessToken(UserDetails userDetails) {
        var now = System.currentTimeMillis();
        var roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority) // e.g. "ROLE_USER"
                .collect(Collectors.toList());
        User user = (User) userDetails;

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("roles", roles)
                .claim("userId", user.getId())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + getAccessTokenExpirationSeconds()))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // generate refresh token (no roles/claims required)
    public String generateRefreshToken(UserDetails userDetails) {
        var now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + refreshTokenExpirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public int getAccessTokenExpirationSeconds() {
        long seconds = accessTokenExpirationMs / 1000L;
        return (int) Math.min(seconds, Integer.MAX_VALUE);
    }

    public int getRefreshTokenExpirationSeconds() {
        long seconds = refreshTokenExpirationMs / 1000L;
        return (int) Math.min(seconds, Integer.MAX_VALUE);
    }

    // extract username
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder().setSigningKey(signingKey).build()
                .parseClaimsJws(token).getBody();
        return claimsResolver.apply(claims);
    }

    public boolean isTokenExpired(String token) {
        final Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
}