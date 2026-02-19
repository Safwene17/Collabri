package org.example.calendarservice.entites;

import jakarta.persistence.*;
import lombok.*;
import org.example.calendarservice.enums.Visibility;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "calendars")
public class Calendar {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    private String timeZone;

    @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    // ← LAZY for perf
    private List<Member> members = new ArrayList<>();  // ← Set for unique members (no duplicates)

    @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Event> events = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void addMember(Member member) {
        if (members.contains(member)) return;  // Set prevents duplicates
        members.add(member);
        member.setCalendar(this);
    }

    public void removeMember(Member member) {
        members.remove(member);
        member.setCalendar(null);
    }

    // Similar for tasks/events
    public void addTask(Task task) {
        tasks.add(task);
        task.setCalendar(this);
    }

    public void addEvent(Event event) {
        events.add(event);
        event.setCalendar(this);
    }
}