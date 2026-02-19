package org.example.calendarservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.calendarservice.enums.Visibility;

public record CalendarRequest(
        @NotBlank(message = "Name cannot be empty")
        String name,

        String description,

        @NotNull(message = "Visibility cannot be empty")
        Visibility visibility,

        @NotBlank(message = "Visibility cannot be empty")
        String timeZone
) {

}
