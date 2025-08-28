package org.example.calendarservice.controllers;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.dto.CalendarRequest;
import org.example.calendarservice.dto.CalendarResponse;
import org.example.calendarservice.services.CalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendars")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService service;

    @PostMapping
    public ResponseEntity<Void> createCategory(@RequestBody CalendarRequest request) {
        return ResponseEntity.ok(service.createCalendar(request));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CalendarResponse>> searchPublic(@RequestParam(required = false) String name) {
        return ResponseEntity.ok(service.searchPublicCalendars(name));
    }


}
