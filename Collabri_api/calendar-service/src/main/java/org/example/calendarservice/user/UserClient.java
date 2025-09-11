package org.example.calendarservice.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;
import java.util.UUID;

@FeignClient(name = "user-service", url = "http://localhost:8222/api/v1/users")
public interface UserClient {

    @GetMapping("/get/{id}")
    Optional<UserResponse> findUserbyId(@PathVariable("id") UUID id);

    @GetMapping("/by-email")
    Optional<UserResponse> findByEmail(@RequestParam("email") String email);
}
