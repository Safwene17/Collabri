package org.example.calendarservice.entites;

import jakarta.persistence.*;
import lombok.*;
import org.example.calendarservice.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;
    private String displayName;

    @ManyToOne
    @JoinColumn(name = "calendar_id", nullable = false)
    private Calendar calendar;

    @Enumerated(EnumType.STRING)
    private Role role=Role.VIEWER;

    private LocalDateTime invitedAt;
    private LocalDateTime joinedAt;
    private boolean accepted;
}
