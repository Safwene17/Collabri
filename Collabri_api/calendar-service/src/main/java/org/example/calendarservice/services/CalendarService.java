package org.example.calendarservice.services;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.dto.CalendarRequest;
import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.repositories.CalendarRepository;
import org.springframework.stereotype.Service;

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
}
