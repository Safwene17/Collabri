package org.example.userservice.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.*;
import org.example.userservice.entities.User;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    /**
     * Register user (create record and send verification email).
     * All validation should be done by controller/DTO constraints; keep extra checks here.
     */


    @Transactional
    public void delete(UUID id) {
        userRepository.deleteById(id);
    }

    public UserResponse findById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::fromUser)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
    }

    public PageResponse<UserResponse> findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> result = userRepository.findAll(pageable);

        List<UserResponse> users = result.getContent().stream()
                .map(userMapper::fromUser)
                .toList();

        return new PageResponse<>(users, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public void update(UUID id, RegisterRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (request.firstname() != null && !request.firstname().isBlank()) user.setFirstname(request.firstname());
        if (request.lastname() != null && !request.lastname().isBlank()) user.setLastname(request.lastname());
        if (request.email() != null && !request.email().isBlank() && !user.getEmail().equals(request.email())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new CustomException("Email already in use", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            user.setEmail(request.email());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        userRepository.save(user);
    }

    public UserResponse findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::fromUser)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
    }
}
