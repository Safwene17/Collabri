package org.example.calendarservice.repositories;

import org.example.calendarservice.entites.Member;
import org.example.calendarservice.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    boolean existsByUserIdAndCalendarId(UUID userId, UUID calendarId);

    Optional<Member> findByUserIdAndCalendarId(UUID callerUserId, UUID calendarId);

    boolean existsByCalendarIdAndEmailIgnoreCase(UUID calendarId, String email);

    List<Member> findAllByCalendarId(UUID calendarId);

    List<Member> findAllByRoleIn(List<Role> roles);

    boolean existsByIdAndCalendarId(UUID memberId, UUID calendarId);

    Optional<Member> findByUserId(UUID userId);
}
