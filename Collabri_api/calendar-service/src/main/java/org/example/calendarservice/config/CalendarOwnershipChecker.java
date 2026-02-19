// file: src/main/java/org/example/calendarservice/config/CalendarOwnershipChecker.java
package org.example.calendarservice.config;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.enums.Role;
import org.example.calendarservice.repositories.CalendarRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("ownershipChecker")
@RequiredArgsConstructor
public class CalendarOwnershipChecker {

    private final CalendarRepository calendarRepository;

    public boolean isOwner(UUID calendarId, Authentication auth) {
        // ADDED: Bypass for admins (full access)
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }

        String userIdStr = auth.getName();  // Now uses auth.name (userId or adminId)
        try {
            UUID userId = UUID.fromString(userIdStr);
            return calendarRepository.findById(calendarId)
                    .map(cal -> cal.getOwnerId().equals(userId))
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasAccess(UUID calendarId, Authentication auth, String requiredRole) {
        // ADDED: Bypass for admins (full access)
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) || auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))) {
            return true;
        }

        String userIdStr = auth.getName();  // Now uses auth.name (userId or adminId)
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