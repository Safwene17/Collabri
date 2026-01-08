package org.example.calendarservice.repositories;

import org.example.calendarservice.entites.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findAllByCalendarId(UUID calendarId);
}