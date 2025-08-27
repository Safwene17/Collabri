package org.example.userservice.services;

import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.entities.User;
import org.example.userservice.repositories.UserRepository;
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
public class UserService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public Void register(RegisterRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }
        User user = mapper.toUser(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        repository.save(user);
        return null;
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
}
