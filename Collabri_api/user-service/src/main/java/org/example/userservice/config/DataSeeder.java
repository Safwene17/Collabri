package org.example.userservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    @Value("${app.admin-email}")
    private String adminEmail;

    @Value("${app.admin-password}")
    private String adminPassword;

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByRole(Role.SUPER_ADMIN)) {
            User initialAdmin = User.builder()
                    .firstname("Super ")
                    .lastname("Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))  // Change this in prod; prompt to reset on first login
                    .role(Role.SUPER_ADMIN)
                    .verified(true)
                    .build();

            userRepository.save(initialAdmin);
        }
    }
}