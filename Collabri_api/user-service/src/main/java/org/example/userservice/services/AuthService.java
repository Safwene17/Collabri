// file: src/main/java/org/example/userservice/services/AuthService.java
package org.example.userservice.services;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.entities.RefreshToken;
import org.example.userservice.entities.User;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.jwt.JwtService;
import org.example.userservice.jwt.RefreshTokenService;
import org.example.userservice.jwt.TokenService;
import org.example.userservice.mappers.UserMapper;
import org.example.userservice.repositories.AdminRepository;
import org.example.userservice.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;          // direct read to get entity after auth
    private final CustomUserDetailsService userDetailsService;
    private final TokenService tokenService;              // issues access string + refresh cookie
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AdminRepository adminRepository;  // CHANGED: Added to check for email uniqueness

    /**
     * Register — delegate to userService which handles mapping, persistence and send verification email.
     */
    public void register(RegisterRequest request) {
        if (adminRepository.existsByEmail(request.email()) || userRepository.existsByEmail(request.email())) {
            throw new CustomException("Email already exists", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        try {
            emailVerificationService.createAndSendVerificationToken(user.getEmail());
        } catch (Exception ex) {
            log.warn("Failed to send verification email for {}", user.getEmail(), ex);
            throw new CustomException("Failed to send verification email", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (Exception ex) {
            throw new CustomException("Invalid credentials", HttpStatus.BAD_REQUEST);
        }

        // fetch user entity
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = tokenService.issueTokens(userDetails, response);
        return new LoginResponse(accessToken);
    }


    public LoginResponse refresh(String refreshTokenValue, HttpServletResponse response) {
        RefreshToken oldToken = refreshTokenService.findByToken(refreshTokenValue);
        refreshTokenService.verifyExpiration(oldToken);
        refreshTokenService.revokeOtherTokens(oldToken);
        User user = oldToken.getUser();
        if (user == null) {
            throw new CustomException("Invalid refresh token for user", HttpStatus.UNAUTHORIZED);  // ADDED: Type check
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccess = tokenService.rotateRefreshToken(oldToken, userDetails, response);
        return new LoginResponse(newAccess);
    }

    public void logout(String refreshTokenValue, String authorizationHeader, HttpServletResponse response) {
        // NEW: Validate access token from header
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException("Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }
        String accessToken = authorizationHeader.substring(7).trim();
        if (accessToken.isEmpty()) {
            throw new CustomException("Missing access token", HttpStatus.UNAUTHORIZED);
        }

        // Validate token using JwtService
        try {
            if (jwtService.isTokenExpired(accessToken)) {
                throw new CustomException("Access token expired", HttpStatus.UNAUTHORIZED);
            }
            // Optional: Extract username from token and match against refresh token's user for extra security
            String tokenUsername = jwtService.extractUsername(accessToken);
            if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
                RefreshToken token = refreshTokenService.findByToken(refreshTokenValue);
                User user = token.getUser();
                if (user == null || !user.getEmail().equals(tokenUsername)) {  // CHANGED: Add type check
                    throw new CustomException("Token mismatch", HttpStatus.UNAUTHORIZED);
                }
            }
        } catch (Exception e) {
            log.warn("Invalid access token during logout: {}", e.getMessage());
            throw new CustomException("Invalid access token", HttpStatus.UNAUTHORIZED);
        }

        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            try {
                RefreshToken token = refreshTokenService.findByToken(refreshTokenValue);
                refreshTokenService.revokeToken(token);
                tokenService.clearRefreshToken(token.getUser(), response);
            } catch (Exception ignored) {
                log.warn("Failed to revoke refresh token {}", refreshTokenValue, ignored);

            }
        } else {
            // still clear cookie even if token missing
            tokenService.clearRefreshToken(null, response);
        }
    }
}