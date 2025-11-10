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
     * Issue access + refresh token cookies for a user.
     */
    public void issueTokens(User user, UserDetails userDetails, HttpServletResponse response, boolean secure, boolean httpOnly) {

        // 1️⃣ Generate JWT access token
        String accessToken = jwtService.generateAccessToken(userDetails);

        // 2️⃣ Generate refresh token and persist
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // 3️⃣ Create cookies
        ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", accessToken)
                .httpOnly(httpOnly)
                .secure(secure)
                .path("/")
                .maxAge(jwtService.getAccessTokenExpirationSeconds())
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken.getToken())
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(jwtService.getRefreshTokenExpirationSeconds())
                .build();

        // 4️⃣ Add cookies to response
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    /**
     * Rotate refresh token and update cookies.
     */
    public void rotateRefreshToken(RefreshToken oldToken, UserDetails userDetails, HttpServletResponse response, boolean secure, boolean httpOnly) {
        refreshTokenService.revokeToken(oldToken);
        issueTokens(oldToken.getUser(), userDetails, response, secure, httpOnly);
    }

    /**
     * Revoke all tokens and clear cookies (logout).
     */
    public void revokeTokens(User user, HttpServletResponse response, boolean secure) {
        refreshTokenService.revokeAllTokensForUser(user);

        ResponseCookie clearAccess = ResponseCookie.from("ACCESS_TOKEN", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie clearRefresh = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", clearAccess.toString());
        response.addHeader("Set-Cookie", clearRefresh.toString());
    }
}
