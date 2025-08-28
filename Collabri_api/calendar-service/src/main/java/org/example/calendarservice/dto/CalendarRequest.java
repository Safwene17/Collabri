package org.example.calendarservice.dto;


import jakarta.validation.constraints.NotNull;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.enums.Visibility;

import java.util.List;
import java.util.UUID;

public record CalendarRequest(
        @NotNull(message = "Name cannot be null")
        String name,

        String description,

        @NotNull(message = "OwnerId cannot be null")
        UUID ownerId,

        @NotNull(message = "Visibility cannot be null")
        Visibility visibility,

        String timeZone,

        List<Member> members
) {

}
