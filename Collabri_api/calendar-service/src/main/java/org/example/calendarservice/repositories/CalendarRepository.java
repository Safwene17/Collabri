package org.example.calendarservice.repositories;

import org.example.calendarservice.entites.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, UUID> {

}
