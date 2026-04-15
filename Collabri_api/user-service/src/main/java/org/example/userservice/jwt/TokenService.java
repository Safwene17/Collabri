// file: src/main/java/org/example/userservice/services/TokenService.java
package org.example.userservice.jwt;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.userservice.entities.RefreshToken;
import org.example.userservice.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    public String issueTokens(UserDetails userDetails, HttpServletResponse response) {
        User user = (User) userDetails;
        String accessToken = jwtService.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken.getToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("None")
                .maxAge(Math.min(refreshTokenExpirationMs / 1000L, Integer.MAX_VALUE))
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());
        return accessToken;
    }

    /**
     * Rotate refresh token and issue a new access token.
     */
    @Transactional
    public String rotateRefreshToken(RefreshToken oldToken, UserDetails userDetails, HttpServletResponse response) {
        User user = oldToken.getUser();
        if (user == null) {
            throw new IllegalStateException("Cannot rotate refresh token without user");
        }

        // Keep one valid token per user: revoke existing set, then issue a fresh token.
        refreshTokenService.revokeAllTokensForUser(user);
        return issueTokens(userDetails, response);
    }

    public void clearRefreshToken(UserDetails userDetails, HttpServletResponse response) {
        if (userDetails != null) {
            User user = (User) userDetails;
            refreshTokenService.revokeAllTokensForUser(user);
        }
        ResponseCookie clearCookie = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("None")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", clearCookie.toString());
    }
}