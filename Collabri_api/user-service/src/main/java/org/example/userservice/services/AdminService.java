// file: src/main/java/org/example/userservice/services/AdminService.java
package org.example.userservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.UserRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.jwt.RefreshTokenService;
import org.example.userservice.mappers.UserMapper;
import org.example.userservice.repositories.UserRepository;
import org.springframework.data.domain.Page;  // ADDED
import org.springframework.data.domain.Pageable;  // ADDED
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;  // ADDED: For consistency

import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class AdminService {

    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    //------------------------------ADMIN CRUD--------------------------------//

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void createAdmin(UserRequest request) {
        if (userRepository.existsByEmail(request.email()) || userRepository.existsByEmail(request.email())) {
            throw new CustomException("Email already exists", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        User user = userMapper.toUser(request);
        user.setRole(Role.ADMIN);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::fromUser);
    }

    public UserResponse findById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::fromUser)
                .orElseThrow(() -> new CustomException("Admin not found", HttpStatus.NOT_FOUND));
    }

    public void update(UUID id, UserRequest request) {
        User admin = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("Admin not found", HttpStatus.NOT_FOUND));

        if (request.firstname() != null && !request.firstname().isBlank()) {
            admin.setFirstname(request.firstname());
        }
        if (request.lastname() != null && !request.lastname().isBlank()) {
            admin.setLastname(request.lastname());
        }
        if (request.email() != null && !request.email().isBlank()) {
            if (userRepository.existsByEmail(request.email()) && !request.email().equals(admin.getEmail())
                    || userRepository.existsByEmail(request.email())) {
                throw new CustomException("Email already exists", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            admin.setEmail(request.email());
        }
        if (request.password() != null && !request.password().isBlank()) {
            admin.setPassword(passwordEncoder.encode(request.password()));
        }

        userRepository.save(admin);
    }

    public void delete(UUID id) {
        User admin = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("Admin not found", HttpStatus.NOT_FOUND));
        refreshTokenService.revokeAllTokensForUser(admin);  // ADDED: Revoke/delete all associated refresh tokens
        userRepository.delete(admin);  // CHANGED: Delete the loaded entity
    }

}