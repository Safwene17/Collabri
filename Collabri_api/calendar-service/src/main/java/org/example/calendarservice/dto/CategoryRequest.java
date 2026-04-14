package org.example.calendarservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category name cannot be empty")
        @Size(max = 100, message = "Category name must be at most 100 characters")
        String name,

        @NotBlank(message = "Category description cannot be empty")
        @Size(max = 500, message = "Category description must be at most 500 characters")
        String description
) {
}
