package org.example.userservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.entities.Admin;
import org.example.userservice.enums.Role;
import org.example.userservice.repositories.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (adminRepository.count() == 0) {
            Admin initialAdmin = Admin.builder()
                    .name("Super Admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("StrongInitialPassword123!"))  // Change this in prod; prompt to reset on first login
                    .role(Role.ADMIN)
                    .build();

            adminRepository.save(initialAdmin);
            log.info("Initial admin created: Email=admin@example.com. Please change password immediately.");
        }
    }
}