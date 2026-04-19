package org.example.userservice.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.*;
import org.example.userservice.entities.User;
import org.example.userservice.jwt.RefreshTokenService;
import org.example.userservice.mappers.UserMapper;
import org.example.userservice.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void createUser(UserRequest user) {
        if(userRepository.existsByEmail(user.email())) {
            throw new DataIntegrityViolationException("Email already exists");
        }
        User newUser = userMapper.toUser(user);
        newUser.setPassword(passwordEncoder.encode(user.password()));
        userRepository.save(newUser);
    }

    @Transactional
    public void delete(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        refreshTokenService.revokeAllTokensForUser(user);
        userRepository.delete(user);
    }

    public UserResponse findById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::fromUser)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    public Page<UserResponse> findAllPaginated(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return page.map(userMapper::fromUser);
    }

    @Transactional
    public void update(UUID id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        
        // Only check email availability if the new email is different from the current one
        if (request.email() != null && !request.email().isBlank() && !user.getEmail().equals(request.email())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new DataIntegrityViolationException("Email already in use");
            }
            user.setEmail(request.email());
        }
        
        if (request.firstname() != null && !request.firstname().isBlank()) user.setFirstname(request.firstname());
        if (request.lastname() != null && !request.lastname().isBlank()) user.setLastname(request.lastname());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        userRepository.save(user);
    }

    public UserResponse findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::fromUser)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }
}
