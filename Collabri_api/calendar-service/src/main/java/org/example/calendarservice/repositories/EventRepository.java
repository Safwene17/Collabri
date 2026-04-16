package org.example.calendarservice.repositories;

import org.example.calendarservice.entites.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findAllByCalendarId(UUID calendarId);

    @Query("""
            select count(e)
            from Event e
            where e.createdBy = :userId
              and (e.calendar.ownerId = :userId
                   or exists (select 1 from Member m where m.calendar = e.calendar and m.userId = :userId))
            """)
    long countCreatedByAccessible(@Param("userId") UUID userId);

    @Query("""
            select count(e)
            from Event e
            where e.startTime > :now
              and (e.calendar.ownerId = :userId
                   or exists (select 1 from Member m where m.calendar = e.calendar and m.userId = :userId))
            """)
    long countUpcomingAccessible(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}