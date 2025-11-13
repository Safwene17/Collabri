package org.example.userservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.ApiResponse;
import org.example.userservice.dto.PageResponse;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable("id") UUID id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully", null));
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable("id") UUID id) {
        UserResponse user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok("User fetched successfully", user));
    }

    @GetMapping("/by-email")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@RequestParam("email") String email) {
        UserResponse user = userService.findByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok("User fetched successfully", user));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        PageResponse<UserResponse> users = userService.findAllPaginated(page, size);
        return ResponseEntity.ok(ApiResponse.ok("Users fetched successfully", users));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<Void>> updateUser(@PathVariable UUID id,
                                                        @RequestBody @Valid RegisterRequest request) {
        userService.update(id, request);
        return ResponseEntity.accepted().body(ApiResponse.ok("User updated successfully", null));
    }
}
