package org.example.calendarservice.controllers;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.shaded.com.google.protobuf.Api;
import org.example.calendarservice.dto.AcceptInviteRequest;
import org.example.calendarservice.dto.ApiResponse;
import org.example.calendarservice.dto.DeclineInviteRequest;
import org.example.calendarservice.services.CalendarInviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invites")
@RequiredArgsConstructor
public class CalendarInviteController {

    private final CalendarInviteService inviteService;

    @PostMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> inviteByEmail(@PathVariable("id") UUID calendarId, @RequestParam String destinationEmail, Authentication authentication) {
        inviteService.inviteMember(calendarId, destinationEmail, authentication);
        return ResponseEntity.status(201).body(ApiResponse.ok("Invitation sent successfully", null));
    }

    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInviteAuthenticated(@RequestBody AcceptInviteRequest req, Authentication authentication) {
        inviteService.acceptInviteWithAuth(req.token(), authentication);
        return ResponseEntity.ok((ApiResponse.ok("Invite accepted successfully", null)));
    }

    @PostMapping("/decline")
    public ResponseEntity<ApiResponse<Void>> declineInvite(@RequestBody DeclineInviteRequest req, Authentication authentication) {
        inviteService.declineInvite(req.token(), authentication);
        return ResponseEntity.ok(ApiResponse.ok("Invitation declined", null));
    }


}
