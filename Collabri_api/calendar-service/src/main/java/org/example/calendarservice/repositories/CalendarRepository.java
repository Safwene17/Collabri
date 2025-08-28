package org.example.calendarservice.repositories;

import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.enums.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, UUID> {
    List<Calendar> findByVisibilityAndNameStartingWithIgnoreCase(Visibility visibility, String name);

    List<Calendar> findByVisibility(Visibility visibility);
}
