package org.example.calendarservice.dto;

import org.example.calendarservice.entites.Event;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.entites.Task;
import org.example.calendarservice.enums.Visibility;

import java.util.List;
import java.util.UUID;

public record CalendarResponse(
        UUID id,
        String name,
        String description,
        UUID ownerId,
        Visibility visibility,
        String timeZone,
        List<Member> members,
        List<Task> tasks,
        List<Event> events
) {
}
