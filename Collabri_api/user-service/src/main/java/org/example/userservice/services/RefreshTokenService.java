package org.example.userservice.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.userservice.entities.RefreshToken;
import org.example.userservice.entities.User;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.repositories.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public void verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(Instant.now()) || token.isRevoked()) {
            refreshTokenRepository.delete(token);
            throw new CustomException("Refresh token expired or revoked", HttpStatus.UNAUTHORIZED);
        }
    }

    public void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    public void revokeAllTokensForUser(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }

    @Transactional
    public void revokeOtherTokens(RefreshToken current) {
        if (current == null || current.getUser() == null) return;
        refreshTokenRepository.revokeAllExcept(current.getUser(), current.getToken());
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new CustomException("Refresh token not found", HttpStatus.UNAUTHORIZED));
    }
}
