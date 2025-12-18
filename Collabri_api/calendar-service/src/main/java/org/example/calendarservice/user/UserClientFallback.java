package org.example.calendarservice.user;

import org.example.calendarservice.exceptions.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserClientFallback implements UserClient {
    @Override
    public Optional<UserResponse> findUserbyId(UUID id) {
        throw new CustomException("User service is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public Optional<UserResponse> findByEmail(String email) {
        throw new CustomException("User service is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }
}