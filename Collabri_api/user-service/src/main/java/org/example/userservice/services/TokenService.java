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
    public String issueTokens(User user, UserDetails userDetails, HttpServletResponse response, boolean secure) {

        // 1️⃣ Generate access token
        String accessToken = jwtService.generateAccessToken(userDetails);

        // 2️⃣ Generate & persist refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // 3️⃣ Create refresh token cookie only
        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken.getToken())
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Strict")
                .maxAge(jwtService.getRefreshTokenExpirationSeconds())
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        // 4️⃣ Return access token for frontend
        return accessToken;
    }

    /**
     * Rotate refresh token and issue a new access token.
     */
    public String rotateRefreshToken(RefreshToken oldToken, UserDetails userDetails, HttpServletResponse response, boolean secure) {
        refreshTokenService.revokeToken(oldToken);
        return issueTokens(oldToken.getUser(), userDetails, response, secure);
    }

    /**
     * Logout — clear refresh cookie.
     */
    public void clearRefreshToken(User user, HttpServletResponse response, boolean secure) {
        refreshTokenService.revokeAllTokensForUser(user);

        ResponseCookie clearCookie = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", clearCookie.toString());
    }
}
