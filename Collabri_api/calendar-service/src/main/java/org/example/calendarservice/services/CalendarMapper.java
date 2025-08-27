package org.example.calendarservice.services;

import org.example.calendarservice.dto.CalendarRequest;
import org.example.calendarservice.entites.Calendar;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CalendarMapper {

    public Calendar toCalendar(CalendarRequest request) {
        var calendar = new Calendar();
        return calendar.builder()
                .name(request.name())
                .description(request.description())
                .ownerId(request.ownerId())
                .members(request.members())
                .timeZone(request.timeZone())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
