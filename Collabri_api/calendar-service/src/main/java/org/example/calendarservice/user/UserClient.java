package org.example.calendarservice.user;

import org.example.calendarservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;
import java.util.UUID;

@FeignClient(name = "user", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/api/v1/users/get/{id}")
    Optional<UserResponse> findUserbyId(@PathVariable("id") UUID id);

    @GetMapping("/api/v1/users/by-email")
    Optional<UserResponse> findByEmail(@RequestParam("email") String email);
}
