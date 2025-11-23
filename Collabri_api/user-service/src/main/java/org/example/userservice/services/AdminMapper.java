package org.example.userservice.services;

import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.AdminRequest;
import org.example.userservice.dto.AdminResponse;
import org.example.userservice.entities.Admin;
import org.springframework.stereotype.Service;

@Service
public class AdminMapper {

    public Admin toAdmin(AdminRequest request) {
        if (request == null) {
            return null;
        }
        return Admin.builder()
                .name(request.name())
                .email(request.email())
                .password(request.password())
                .build();
    }

    public AdminResponse fromAdmin(Admin admin) {
        return new AdminResponse(
                admin.getId(),
                admin.getName(),
                admin.getEmail(),
                admin.getPassword()
        );
    }
}
