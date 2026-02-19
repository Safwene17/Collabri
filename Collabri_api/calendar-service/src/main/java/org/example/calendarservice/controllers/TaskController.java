package org.example.calendarservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.calendarservice.dto.ApiResponse;
import org.example.calendarservice.dto.TaskRequest;
import org.example.calendarservice.dto.TaskResponse;
import org.example.calendarservice.services.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createTask(@RequestBody @Valid TaskRequest request, @RequestParam UUID calendarId, Authentication authentication) {
        taskService.createTask(request, calendarId, authentication);
        return ResponseEntity.status(201).body(ApiResponse.ok("Task created successfully", null));
    }

    @GetMapping("/calendar/{calendarId}")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByCalendar(@PathVariable UUID calendarId) {
        List<TaskResponse> tasks = taskService.getTasksByCalendar(calendarId);
        return ResponseEntity.ok(ApiResponse.ok("Tasks retrieved successfully", tasks));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateTask(@RequestBody @Valid TaskRequest request, @PathVariable UUID id, @RequestParam UUID calendarId) {
        taskService.updateTask(request, id, calendarId);
        return ResponseEntity.accepted().body(ApiResponse.ok("Task updated successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable UUID id, @RequestParam UUID calendarId) {
        taskService.deleteTask(id, calendarId);
        return ResponseEntity.ok(ApiResponse.ok("Task deleted successfully", null));
    }
}