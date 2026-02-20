package org.example.calendarservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.calendarservice.dto.ApiResponse;
import org.example.calendarservice.dto.CalendarRequest;
import org.example.calendarservice.dto.CalendarResponse;
import org.example.calendarservice.services.CalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendars")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public ResponseEntity<List<CalendarResponse>> getAllCalendars() {
        return ResponseEntity.ok(calendarService.getAllCalendars());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createCalendar(@RequestBody @Valid CalendarRequest request, Authentication authentication) {
        calendarService.createCalendar(request, authentication);
        return ResponseEntity.status(201).body(ApiResponse.ok("Calendar created successfully", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CalendarResponse>> getCalendarById(@PathVariable("id") UUID id) {
        CalendarResponse calendar = calendarService.getCalendarById(id);
        return ResponseEntity.ok(ApiResponse.ok("Calendar retrieved successfully", calendar));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCalendarById(@PathVariable("id") UUID id, Authentication authentication) {
        calendarService.deleteCalendarById(id, authentication);
        return ResponseEntity.status(204).body(ApiResponse.ok("Calendar deleted successfully", null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateCalendar(@RequestBody @Valid CalendarRequest request, @PathVariable("id") UUID id , Authentication authentication) {
        calendarService.updateCalendar(request, id, authentication);
        return ResponseEntity.accepted().body((ApiResponse.ok("Calendar updated successfully", null)));
    }

    //turn this into pageable in the future
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CalendarResponse>>> searchPublic(@RequestParam(required = false) String name) {
        List<CalendarResponse> list = calendarService.searchPublicCalendars(name);
        return ResponseEntity.ok(ApiResponse.ok(("Public calendars retrieved successfully"), list));
    }

}
