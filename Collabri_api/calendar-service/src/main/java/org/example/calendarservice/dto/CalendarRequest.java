package org.example.calendarservice.dto;


import org.example.calendarservice.entites.Member;

import java.util.List;
import java.util.UUID;

public record CalendarRequest(
        String name,

        String description,

        UUID ownerId,

        String timeZone,

        List<Member> members
) {

}
