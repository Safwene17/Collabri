package org.example.calendarservice.controllers;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.services.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService service;

    @PostMapping("/{calendarId}/join/{userId}")
    public ResponseEntity<Void> joinPublicCalendar(@PathVariable("calendarId") UUID calendarId, @PathVariable("userId") UUID userId) {
        service.joinPublicCalendar(calendarId, userId);
        return ResponseEntity.ok().build();
    }
}
