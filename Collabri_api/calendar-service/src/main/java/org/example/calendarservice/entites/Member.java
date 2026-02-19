package org.example.calendarservice.entites;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.example.calendarservice.enums.Role;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @NonNull
    private UUID userId;
    private String email;
    private String displayName;

    @ManyToOne
    @JoinColumn(name = "calendar_id", nullable = false)
    @JsonIgnore
    private Calendar calendar;

    @OneToMany
    @JoinColumn(name = "assigned_to")
    private List<Task> assignedTasks = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Role role = Role.VIEWER;

}
