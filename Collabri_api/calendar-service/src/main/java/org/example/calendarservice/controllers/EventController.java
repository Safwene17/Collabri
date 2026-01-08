package org.example.calendarservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.calendarservice.dto.ApiResponse;
import org.example.calendarservice.dto.EventRequest;
import org.example.calendarservice.dto.EventResponse;
import org.example.calendarservice.services.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createEvent(@RequestBody @Valid EventRequest request, @RequestParam UUID calendarId) {
        eventService.createEvent(request, calendarId);
        return ResponseEntity.status(201).body(ApiResponse.ok("Event created successfully", null));
    }

    @GetMapping("/calendar/{calendarId}")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getEventsByCalendar(@PathVariable UUID calendarId) {
        List<EventResponse> events = eventService.getEventsByCalendar(calendarId);
        return ResponseEntity.ok(ApiResponse.ok("Events retrieved successfully", events));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateEvent(@RequestBody @Valid EventRequest request, @PathVariable UUID id, @RequestParam UUID calendarId) {
        eventService.updateEvent(request, id, calendarId);
        return ResponseEntity.ok(ApiResponse.ok("Event updated successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable UUID id, @RequestParam UUID calendarId) {
        eventService.deleteEvent(id, calendarId);
        return ResponseEntity.ok(ApiResponse.ok("Event deleted successfully", null));
    }
}