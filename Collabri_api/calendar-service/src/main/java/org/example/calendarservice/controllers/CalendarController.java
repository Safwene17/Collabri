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

    private final CalendarService service;

    @PostMapping
    public ResponseEntity<Void> createCategory(@RequestBody CalendarRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.createCalendar(request, authentication));
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<CalendarResponse> getCalendarById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.getCalendarById(id));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteCalendarById(@PathVariable("id") UUID id, Authentication authentication) {
        service.deleteCalendarById(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Void> updateCalendar(@RequestBody CalendarRequest request, @PathVariable("id") UUID id, Authentication authentication) {
        service.updateCalendar(request, id, authentication);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<CalendarResponse>> searchPublic(@RequestParam(required = false) String name) {
        return ResponseEntity.ok(service.searchPublicCalendars(name));
    }

    @PostMapping("/invite/{id}")
    public ResponseEntity<Void> inviteByEmail(@PathVariable("id") UUID calendarId, @RequestParam String email, Authentication authentication) {
        service.inviteMemberByEmail(calendarId, email, authentication);
        return ResponseEntity.ok().build();
    }
}
