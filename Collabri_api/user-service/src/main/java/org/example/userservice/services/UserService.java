package org.example.userservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.*;
import org.example.userservice.entities.User;
import org.example.userservice.exceptions.EmailAlreadyUsedException;
import org.example.userservice.exceptions.InvalidEmailFormatException;
import org.example.userservice.exceptions.InvalidPasswordFormatException;
import org.example.userservice.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final EmailVerificationService emailVerificationService;

    public void register(RegisterRequest request) {
        String email = request.email();
        String password = request.password();

        if (email == null || email.isBlank() || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new InvalidEmailFormatException();
        }
        if (repository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        if (password == null || password.isBlank() || !password.matches("(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}")) {
            throw new InvalidPasswordFormatException();
        }

        User user = mapper.toUser(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        repository.save(user);

        try {
            emailVerificationService.createAndSendVerificationToken(user.getEmail());
        } catch (Exception ex) {
            log.warn("Failed to send verification email for {}", user.getEmail(), ex);
        }
    }


    public LoginResponse authenticate(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid credentials");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return new LoginResponse(accessToken, refreshToken);
    }


    public void delete(UUID id) {
        repository.deleteById(id);
    }

    public UserResponse findById(UUID id) {
        return repository.findById(id)
                .map(mapper::fromUser)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(mapper::fromUser).collect(Collectors.toList());
    }

    public void update(UUID id, RegisterRequest request) {
        var user = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.firstname() != null && !request.firstname().isBlank()) {
            user.setFirstname(request.firstname());
        }
        if (request.lastname() != null && !request.lastname().isBlank()) {
            user.setLastname(request.lastname());
        }
        if (request.email() != null && !request.email().isBlank()) {
            if (!user.getEmail().equals(request.email()) && repository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(request.email());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        repository.save(user);
    }

    public UserResponse findByEmail(String email) {
        return repository.findByEmail(email)
                .map(mapper::fromUser)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
