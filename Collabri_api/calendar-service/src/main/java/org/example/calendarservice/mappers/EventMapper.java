package org.example.calendarservice.mappers;

import org.example.calendarservice.dto.EventRequest;
import org.example.calendarservice.dto.EventResponse;
import org.example.calendarservice.entites.Event;
import org.springframework.stereotype.Service;

@Service
public class EventMapper {

    public Event toEvent(EventRequest request) {
        return Event.builder()
                .title(request.title())
                .description(request.description())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .location(request.location())
                .build();
    }

    public EventResponse fromEvent(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCalendar().getId(),
                event.getStartTime(),
                event.getEndTime(),
                event.getLocation(),
                event.getCreatedAt()
        );
    }
}