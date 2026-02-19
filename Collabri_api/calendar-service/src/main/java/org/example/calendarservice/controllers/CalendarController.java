package org.example.calendarservice.controllers;

import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<Void> createCalendar(@RequestBody CalendarRequest request, Authentication authentication) {
        return ResponseEntity.ok(calendarService.createCalendar(request, authentication));
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<CalendarResponse> getCalendarById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(calendarService.getCalendarById(id));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteCalendarById(@PathVariable("id") UUID id, Authentication authentication) {
        calendarService.deleteCalendarById(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Void> updateCalendar(@RequestBody CalendarRequest request, @PathVariable("id") UUID id, Authentication authentication) {
        calendarService.updateCalendar(request, id, authentication);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<CalendarResponse>> searchPublic(@RequestParam(required = false) String name) {
        return ResponseEntity.ok(calendarService.searchPublicCalendars(name));
    }

}
