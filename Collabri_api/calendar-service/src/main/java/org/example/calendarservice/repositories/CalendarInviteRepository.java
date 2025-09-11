package org.example.calendarservice.repositories;

import org.example.calendarservice.entites.CalendarInvite;
import org.example.calendarservice.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalendarInviteRepository extends JpaRepository<CalendarInvite, UUID> {

    Optional<CalendarInvite> findByCalendarIdAndDestinationEmailIgnoreCase(UUID calendarId, String email);

    Optional<CalendarInvite> findByTokenHash(String tokenHash);      // used by accept/decline by token
    
}
