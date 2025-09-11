package org.example.calendarservice.repositories;

import com.netflix.appinfo.ApplicationInfoManager;
import org.example.calendarservice.entites.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    boolean existsByUserIdAndCalendarId(UUID userId, UUID calendarId);

    Optional<Member> findByUserIdAndCalendarId(UUID callerUserId, UUID calendarId);

    boolean existsByCalendarIdAndEmailIgnoreCase(UUID calendarId, String email);
}
