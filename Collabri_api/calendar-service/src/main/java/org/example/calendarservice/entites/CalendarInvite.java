package org.example.calendarservice.entites;

import jakarta.persistence.*;
import lombok.*;
import org.example.calendarservice.enums.InviteStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarInvite {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "calendar_id", columnDefinition = "uuid", nullable = false)
    private UUID calendarId;

    private String email;
    private UUID invitedByUserId;
    private String token;
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    private InviteStatus status; // PENDING, ACCEPTED, EXPIRED, CANCELLED
}