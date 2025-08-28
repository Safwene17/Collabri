package org.example.calendarservice.repositories;

import org.example.calendarservice.entites.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    boolean existsByUserIdAndCalendarId(UUID userId, UUID calendarId);

}
