package org.example.calendarservice.mappers;

import org.example.calendarservice.dto.CalendarRequest;
import org.example.calendarservice.dto.CalendarResponse;
import org.example.calendarservice.entites.Calendar;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class CalendarMapper {

    public Calendar toCalendar(CalendarRequest request) {
        return Calendar.builder()
                .name(request.name())
                .description(request.description())
                .visibility(request.visibility())
                .members(new ArrayList<>())
                .timeZone(request.timeZone())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public CalendarResponse fromCalendar(Calendar calendar) {
        return new CalendarResponse(
                calendar.getId(),
                calendar.getCategory().getId(),
                calendar.getName(),
                calendar.getDescription(),
                calendar.getOwnerId(),
                calendar.getVisibility(),
                calendar.getTimeZone(),
                calendar.getMembers(),
                calendar.getTasks(),
                calendar.getEvents()
        );
    }
}
