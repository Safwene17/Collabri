package org.example.calendarservice.controllers;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.services.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService service;

    @PostMapping("/join/{calendarId}")
    public ResponseEntity<Void> joinPublicCalendar(@PathVariable("calendarId") UUID calendarId, Authentication authentication) {
        service.joinPublicCalendar(calendarId, authentication);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/calendar-members/{calendarId}")
    public ResponseEntity<Optional<List<Member>>> getCalendarMembers(@PathVariable("calendarId") UUID calendarId, Authentication authentication) {
        return ResponseEntity.ok(service.getCalendarMembers(calendarId, authentication));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<?> getMemberById(@PathVariable("memberId") Long memberId) {
        return ResponseEntity.ok(service.getMemberById(memberId));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable("memberId") Long memberId, Authentication authentication) {
        service.removeMember(memberId, authentication);
        return ResponseEntity.noContent().build();
    }
}
