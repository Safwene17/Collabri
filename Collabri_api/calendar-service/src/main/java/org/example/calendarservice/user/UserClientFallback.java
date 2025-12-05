package org.example.calendarservice.user;

import org.example.calendarservice.exceptions.CustomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserClientFallback implements UserClient {
    private static final Logger log = LoggerFactory.getLogger(UserClientFallback.class);

    @Override
    public Optional<UserResponse> findUserbyId(UUID id) {
        log.error("Fallback for findUserbyId: User service unavailable");
        throw new CustomException("User service is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public Optional<UserResponse> findByEmail(String email) {
        log.error("Fallback for findByEmail: User service unavailable");
        throw new CustomException("User service is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }
}