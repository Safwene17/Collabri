// Minor fix in CalendarService.java (added logging, used authentication.name for consistency)
package org.example.calendarservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.calendarservice.dto.CalendarRequest;
import org.example.calendarservice.dto.CalendarResponse;
import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.enums.Role;
import org.example.calendarservice.enums.Visibility;
import org.example.calendarservice.exceptions.CustomException;
import org.example.calendarservice.mappers.CalendarMapper;
import org.example.calendarservice.mappers.MemberMapper;
import org.example.calendarservice.repositories.CalendarRepository;
import org.example.calendarservice.user.UserClient;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final MemberMapper memberMapper;
    private final UserClient userClient;
    private final CalendarMapper calendarMapper;

    @PreAuthorize("@verified.isVerified(authentication)")
    public UUID createCalendar(CalendarRequest request, Authentication authentication) {
        String userIdStr = authentication.getName();
        UUID userId = UUID.fromString(userIdStr);
        var userResponse = userClient.findUserbyId(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        Calendar calendar = calendarMapper.toCalendar(request);
        calendar.setOwnerId(userId);
        Member member = memberMapper.toMember(userResponse);
        member.setRole(Role.OWNER);
        calendar.addMember(member);
        calendarRepository.save(calendar);
        log.info("Created calendar {} for user {}", calendar.getId(), userId);
        return calendar.getId();
    }

    @Transactional(readOnly = true)
    public List<CalendarResponse> searchPublicCalendars(String name) {
        List<Calendar> calendars = (name == null || name.isEmpty())
                ? calendarRepository.findByVisibility(Visibility.PUBLIC)
                : calendarRepository.findByVisibilityAndNameStartingWithIgnoreCase(Visibility.PUBLIC, name);

        return calendars.stream()
                .map(calendarMapper::fromCalendar)
                .toList();
    }

    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#id, authentication.name, 'VIEWER')")
    @Transactional(readOnly = true)
    public CalendarResponse getCalendarById(UUID id) {
        return calendarRepository.findById(id)
                .map(calendarMapper::fromCalendar)
                .orElseThrow(() -> new CustomException("Calendar not found", HttpStatus.NOT_FOUND));
    }

    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.isOwner(#id, authentication.name)")
    public void deleteCalendarById(UUID id) {
        if (!calendarRepository.existsById(id)) {
            throw new CustomException("Calendar not found", HttpStatus.NOT_FOUND);
        }
        calendarRepository.deleteById(id);
        log.info("Deleted calendar {}", id);
    }

    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#id, authentication.name, 'MANAGER')")
    public void updateCalendar(CalendarRequest request, UUID id) {
        Calendar calendar = calendarRepository.findById(id)
                .orElseThrow(() -> new CustomException("Calendar not found", HttpStatus.NOT_FOUND));

        calendar.setName(request.name());
        calendar.setDescription(request.description());
        calendar.setVisibility(request.visibility());
        calendar.setTimeZone(request.timeZone());
        calendar.setUpdatedAt(LocalDateTime.now());

        calendarRepository.save(calendar);
        log.info("Updated calendar {}", id);
    }
}