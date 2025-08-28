package org.example.calendarservice.dto;


import jakarta.validation.constraints.NotNull;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.enums.Visibility;

import java.util.List;

public record CalendarRequest(
        @NotNull(message = "Name cannot be null")
        String name,

        String description,

        @NotNull(message = "Visibility cannot be null")
        Visibility visibility,

        String timeZone
) {

}
