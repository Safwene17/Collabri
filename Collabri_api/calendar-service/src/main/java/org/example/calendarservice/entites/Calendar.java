package org.example.calendarservice.entites;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.example.calendarservice.enums.Visibility;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

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
@Table(name = "calendar")
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

    @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Member> members = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void addMember(Member member) {
        members.add(member);
        member.setCalendar(this);
    }

    public void removeMember(Member member) {
        members.remove(member);
        member.setCalendar(null);
    }

}
