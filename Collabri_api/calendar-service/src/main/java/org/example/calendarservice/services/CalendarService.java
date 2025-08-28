package org.example.calendarservice.services;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.dto.CalendarRequest;
import org.example.calendarservice.dto.CalendarResponse;
import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.enums.Visibility;
import org.example.calendarservice.repositories.CalendarRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository repository;
    private final CalendarMapper mapper;

    public Void createCalendar(CalendarRequest request) {
        Calendar calendar = mapper.toCalendar(request);
        repository.save(calendar);
        return null;
    }

    public List<CalendarResponse> searchPublicCalendars(String name) {
        List<Calendar> calendars;

        if (name == null || name.isEmpty()) {
            // Return all public calendars when no name provided
            calendars = repository.findByVisibility(Visibility.PUBLIC);
        } else {
            // Return public calendars with names starting with the provided prefix
            calendars = repository.findByVisibilityAndNameStartingWithIgnoreCase(Visibility.PUBLIC, name);
        }

        return calendars.stream()
                .map(mapper::fromCalendar)
                .toList();
    }
}
