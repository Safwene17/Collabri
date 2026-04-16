// file: src/main/java/org/example/userservice/services/AuthService.java
package org.example.userservice.services;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.dto.UserRequest;
import org.example.userservice.entities.RefreshToken;
import org.example.userservice.entities.User;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.jwt.RefreshTokenService;
import org.example.userservice.jwt.TokenService;
import org.example.userservice.mappers.UserMapper;
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
    public void register(UserRequest request) {
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


    @Transactional
    public LoginResponse refresh(String refreshTokenValue, HttpServletResponse response) {
        RefreshToken oldToken = refreshTokenService.findByToken(refreshTokenValue);
        refreshTokenService.verifyExpiration(oldToken);
        User user = oldToken.getUser();
        if (user == null) {
            throw new CustomException("Invalid refresh token for user", HttpStatus.UNAUTHORIZED);  // ADDED: Type check
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccess = tokenService.rotateRefreshToken(oldToken, userDetails, response);
        return new LoginResponse(newAccess);
    }

    public void logout(String refreshTokenValue, HttpServletResponse response) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new CustomException("Refresh token missing", HttpStatus.UNAUTHORIZED);
        }
        RefreshToken token = refreshTokenService.findByToken(refreshTokenValue);
        UserDetails tokenOwner = token.getUser();
        refreshTokenService.revokeToken(token);
        tokenService.clearRefreshToken(tokenOwner, response);
    }
}