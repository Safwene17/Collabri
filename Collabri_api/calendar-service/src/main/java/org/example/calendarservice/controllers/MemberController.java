// file: src/main/java/org/example/calendarservice/controllers/MemberController.java
package org.example.calendarservice.controllers;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.dto.ApiResponse;
import org.example.calendarservice.dto.MemberResponse;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.enums.Role;
import org.example.calendarservice.services.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService service;

    @PostMapping("/join/{calendarId}")
    public ResponseEntity<ApiResponse<Void>> joinPublicCalendar(@PathVariable UUID calendarId, Authentication authentication) {
        service.joinPublicCalendar(calendarId, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Joined successfully", null));
    }

    @GetMapping("/calendar-members/{calendarId}")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getCalendarMembers(@PathVariable UUID calendarId) {
        List<MemberResponse> members = service.getCalendarMembers(calendarId);  // Fixed: Use DTOs
        return ResponseEntity.ok(ApiResponse.ok("Members retrieved successfully", members));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMemberById(@PathVariable UUID memberId, @RequestParam UUID calendarId) {
        return ResponseEntity.ok(ApiResponse.ok("Member retrieved successfully", service.getMemberById(memberId, calendarId)));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable UUID memberId, @RequestParam UUID calendarId) {
        service.removeMember(memberId, calendarId);
        return ResponseEntity.status(204).body(ApiResponse.ok("Member removed successfully", null));
    }

    @DeleteMapping("/deleteFromCalendar/{memberId}")
    public ResponseEntity<ApiResponse<Void>> deleteMemberFromCalendar(@PathVariable UUID memberId, @RequestParam UUID calendarId) {
        service.removeMemberFromCalendar(memberId, calendarId);
        return ResponseEntity.status(204).body(ApiResponse.ok("Member removed from calendar successfully", null));
    }

    @PutMapping("/set-role/{memberId}")
    public ResponseEntity<ApiResponse<Void>> updateMemberRole(@PathVariable UUID memberId, @RequestParam UUID calendarId, @RequestParam Role role) {
        service.setMemberRole(memberId, calendarId, role);
        return ResponseEntity.accepted().body(ApiResponse.ok("Member role updated successfully", null));
    }
}