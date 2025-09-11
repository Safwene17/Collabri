package org.example.calendarservice.utils;

import org.example.calendarservice.enums.InviteStatus;
import org.example.calendarservice.repositories.CalendarInviteRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class InviteExpiryScheduler {

    private final CalendarInviteRepository inviteRepository;

    // run every hour
    @Scheduled(fixedRateString = "PT1H")
    public void expireInvites() {
        var now = Instant.now();
        var pending = inviteRepository.findAll().stream()
                .filter(i -> i.getStatus() == InviteStatus.PENDING && i.getExpiresAt() != null && i.getExpiresAt().isBefore(now))
                .toList();
        for (var inv : pending) {
            inv.setStatus(InviteStatus.EXPIRED);
            inv.setTokenHash(null); // clear token to prevent reuse
        }
        inviteRepository.saveAll(pending);
    }
}
