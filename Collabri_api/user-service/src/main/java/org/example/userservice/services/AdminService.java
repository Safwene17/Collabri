package org.example.userservice.services;

import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.AdminRequest;
import org.example.userservice.dto.ApiResponse;
import org.example.userservice.entities.Admin;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.repositories.AdminRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminMapper mapper;

    public void createAdmin(AdminRequest request) {
        if (adminRepository.existsByEmail(request.email())) {
            throw new CustomException("Admin with this email already exists", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Admin admin = mapper.toAdmin(request);
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        adminRepository.save(admin);
    }
}
