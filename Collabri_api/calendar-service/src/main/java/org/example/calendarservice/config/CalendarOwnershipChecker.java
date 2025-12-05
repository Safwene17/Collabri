package org.example.calendarservice.config;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.enums.Role;
import org.example.calendarservice.exceptions.CustomException;
import org.example.calendarservice.repositories.CalendarRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("ownershipChecker")
@RequiredArgsConstructor
public class CalendarOwnershipChecker {

    private final CalendarRepository calendarRepository;

    public boolean isOwner(UUID calendarId, String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        Calendar cal = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CustomException("Calendar not found", HttpStatus.NOT_FOUND));
        return cal.getOwnerId().equals(userId);
    }

    public boolean hasAccess(UUID calendarId, String userIdStr, String requiredRole) {
        UUID userId = UUID.fromString(userIdStr);
        Calendar cal = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CustomException("Calendar not found", HttpStatus.NOT_FOUND));
        return cal.getOwnerId().equals(userId) ||
                cal.getMembers().stream()
                        .anyMatch(m -> m.getUserId().equals(userId) &&
                                m.getRole().ordinal() >= Role.valueOf(requiredRole).ordinal());
    }
}