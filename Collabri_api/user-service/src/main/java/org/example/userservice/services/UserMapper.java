package org.example.userservice.services;

import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.entities.User;
import org.springframework.stereotype.Service;

import static org.example.userservice.enums.Role.USER;

@Service
public class UserMapper {

    public User toUser(RegisterRequest request){
        if(request == null) {
            return null;
        }
        return User.builder()
                .firstname(request.firstname())
                .lastname(request.lastname())
                .phoneNumber(request.phoneNumber())
                .email(request.email() )
                .password(request.password())
                .role(USER)
                .build();
    }
}
