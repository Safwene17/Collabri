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

    /**
     * Register — delegate to userService which handles mapping, persistence and send verification email.
     */
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
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

    /**
     * Login: authenticate credentials, issue tokens (access string + refresh cookie).
     */
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (Exception ex) {
            throw new CustomException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        // fetch user entity
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = tokenService.issueTokens(user, userDetails, response);
        return new LoginResponse(accessToken);
    }

    /**
     * Refresh: validate DB refresh token -> rotate -> return new access token (cookie set)
     */

    public LoginResponse refresh(String refreshTokenValue, HttpServletResponse response) {
        RefreshToken oldToken = refreshTokenService.findByToken(refreshTokenValue);
        refreshTokenService.verifyExpiration(oldToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(oldToken.getUser().getEmail());
        String newAccess = tokenService.rotateRefreshToken(oldToken, userDetails, response);
        return new LoginResponse(newAccess);
    }

    /**
     * Logout: revoke token(s) and clear cookie.
     */
    public void logout(String refreshTokenValue, HttpServletResponse response) {
        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            try {
                RefreshToken token = refreshTokenService.findByToken(refreshTokenValue);
                refreshTokenService.revokeToken(token);
                tokenService.clearRefreshToken(token.getUser(), response);
            } catch (Exception ignored) {
                // Why it is Ignored ? No handling ?
            }
        } else {
            // still clear cookie even if token missing
            tokenService.clearRefreshToken(null, response);
        }
    }

}
