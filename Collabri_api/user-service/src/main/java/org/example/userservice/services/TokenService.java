package org.example.userservice.services;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.userservice.entities.RefreshToken;
import org.example.userservice.entities.User;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Issue access token (returned as string) + refresh token cookie.
     */
    public String issueTokens(User user, UserDetails userDetails, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken.getToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Strict")
                .maxAge(jwtService.getRefreshTokenExpirationSeconds())
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());
        return accessToken;
    }

    /**
     * Rotate refresh token and issue a new access token.
     */
    public String rotateRefreshToken(RefreshToken oldToken, UserDetails userDetails, HttpServletResponse response) {
        refreshTokenService.revokeToken(oldToken);
        return issueTokens(oldToken.getUser(), userDetails, response);
    }

    /**
     * Clear refresh cookie on logout and revoke server-side tokens.
     * If user is null, still clear cookie.
     */
    public void clearRefreshToken(User user, HttpServletResponse response) {
        if (user != null) {
            refreshTokenService.revokeAllTokensForUser(user);
        }
        ResponseCookie clearCookie = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", clearCookie.toString());
    }
}
