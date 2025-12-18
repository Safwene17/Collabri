// file: src/main/java/org/example/calendarservice/config/CalendarOwnershipChecker.java
package org.example.calendarservice.config;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.enums.Role;
import org.example.calendarservice.repositories.CalendarRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("ownershipChecker")
@RequiredArgsConstructor
public class CalendarOwnershipChecker {

    private final CalendarRepository calendarRepository;

    public boolean isOwner(UUID calendarId, String userIdStr) {
        try {
            UUID userId = UUID.fromString(userIdStr);
            return calendarRepository.findById(calendarId)
                    .map(cal -> cal.getOwnerId().equals(userId))
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasAccess(UUID calendarId, String userIdStr, String requiredRole) {
        try {
            UUID userId = UUID.fromString(userIdStr);
            Role reqRole = Role.valueOf(requiredRole);
            return calendarRepository.findById(calendarId)
                    .map(cal -> cal.getOwnerId().equals(userId) ||
                            cal.getMembers().stream()
                                    .anyMatch(m -> m.getUserId().equals(userId) &&
                                            m.getRole().ordinal() >= reqRole.ordinal()))
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }
}