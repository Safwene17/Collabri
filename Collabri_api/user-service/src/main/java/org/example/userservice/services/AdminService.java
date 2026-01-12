// file: src/main/java/org/example/userservice/services/AdminService.java
package org.example.userservice.services;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.AdminRequest;
import org.example.userservice.dto.AdminResponse;  // ADDED
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.entities.Admin;
import org.example.userservice.entities.RefreshToken;
import org.example.userservice.enums.Role;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.jwt.JwtService;
import org.example.userservice.jwt.RefreshTokenService;
import org.example.userservice.jwt.TokenService;
import org.example.userservice.mappers.AdminMapper;
import org.example.userservice.repositories.AdminRepository;
import org.example.userservice.repositories.UserRepository;
import org.springframework.data.domain.Page;  // ADDED
import org.springframework.data.domain.Pageable;  // ADDED
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;  // ADDED: For consistency

import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminMapper mapper;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    //------------------------------ADMIN CRUD--------------------------------//

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void createAdmin(AdminRequest request) {
        if (adminRepository.existsByEmail(request.email()) || userRepository.existsByEmail(request.email())) {
            throw new CustomException("Email already exists", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Admin admin = mapper.toAdmin(request);
        admin.setRole(Role.ADMIN);
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        adminRepository.save(admin);
    }

    public Page<AdminResponse> findAll(Pageable pageable) {
        return adminRepository.findAll(pageable).map(mapper::fromAdmin);
    }

    public AdminResponse findById(UUID id) {
        return adminRepository.findById(id)
                .map(mapper::fromAdmin)
                .orElseThrow(() -> new CustomException("Admin not found", HttpStatus.NOT_FOUND));
    }

    public void update(UUID id, AdminRequest request) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new CustomException("Admin not found", HttpStatus.NOT_FOUND));

        if (request.name() != null && !request.name().isBlank()) {
            admin.setName(request.name());
        }
        if (request.email() != null && !request.email().isBlank()) {
            if (adminRepository.existsByEmail(request.email()) && !request.email().equals(admin.getEmail())
                    || userRepository.existsByEmail(request.email())) {
                throw new CustomException("Email already exists", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            admin.setEmail(request.email());
        }
        if (request.password() != null && !request.password().isBlank()) {
            admin.setPassword(passwordEncoder.encode(request.password()));
        }

        adminRepository.save(admin);
    }

    public void delete(UUID id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new CustomException("Admin not found", HttpStatus.NOT_FOUND));
        refreshTokenService.revokeAllTokensForAdmin(admin);  // ADDED: Revoke/delete all associated refresh tokens
        adminRepository.delete(admin);  // CHANGED: Delete the loaded entity
    }

    //------------------------------ADMIN AUTHENTICATION--------------------------------//

    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (Exception ex) {
            throw new CustomException("Invalid credentials", HttpStatus.BAD_REQUEST);
        }

        // fetch admin entity
        Admin admin = adminRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException("Admin not found", HttpStatus.NOT_FOUND));

        UserDetails userDetails = userDetailsService.loadUserByUsername(admin.getEmail());

        String accessToken = tokenService.issueTokens(userDetails, response);
        return new LoginResponse(accessToken);
    }

    // ADDED: Refresh for admins (mirrors user logic but with Admin checks)
    public LoginResponse refresh(String refreshTokenValue, HttpServletResponse response) {
        RefreshToken oldToken = refreshTokenService.findByToken(refreshTokenValue);
        refreshTokenService.verifyExpiration(oldToken);
        refreshTokenService.revokeOtherTokens(oldToken);
        Admin admin = oldToken.getAdmin();
        if (admin == null) {
            throw new CustomException("Invalid refresh token for admin", HttpStatus.UNAUTHORIZED);
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(admin.getEmail());
        String newAccess = tokenService.rotateRefreshToken(oldToken, userDetails, response);
        return new LoginResponse(newAccess);
    }

    // ADDED: Logout for admins (mirrors user logic but with Admin checks)
    public void logout(String refreshTokenValue, String authorizationHeader, HttpServletResponse response) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException("Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }
        String accessToken = authorizationHeader.substring(7).trim();
        if (accessToken.isEmpty()) {
            throw new CustomException("Missing access token", HttpStatus.UNAUTHORIZED);
        }

        try {
            if (jwtService.isTokenExpired(accessToken)) {
                throw new CustomException("Access token expired", HttpStatus.UNAUTHORIZED);
            }
            String tokenUsername = jwtService.extractUsername(accessToken);
            if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
                RefreshToken token = refreshTokenService.findByToken(refreshTokenValue);
                Admin admin = token.getAdmin();
                if (admin == null || !admin.getEmail().equals(tokenUsername)) {
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
                tokenService.clearRefreshToken(token.getAdmin(), response);
            } catch (Exception ignored) {
                log.warn("Failed to revoke refresh token {}", refreshTokenValue, ignored);
            }
        } else {
            tokenService.clearRefreshToken(null, response);
        }
    }

}