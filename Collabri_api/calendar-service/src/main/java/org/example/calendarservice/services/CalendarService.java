package org.example.calendarservice.services;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.dto.CalendarRequest;
import org.example.calendarservice.dto.CalendarResponse;
import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.enums.Role;
import org.example.calendarservice.enums.Visibility;
import org.example.calendarservice.repositories.CalendarRepository;
import org.example.calendarservice.repositories.MemberRepository;
import org.example.calendarservice.user.UserClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final CalendarMapper calendarMapper;
    private final MemberMapper memberMapper;
    private final MemberRepository memberRepository;
    private final UserClient userClient;


    public Void createCalendar(CalendarRequest request, Authentication authentication) {

        // 1. AUTHENTICATION & VALIDATION
        // Extract user ID from JWT token
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaim("userId").toString());

        // Get user details from user service
        var userResponse = userClient.findUserbyId(userId).orElseThrow(() ->
                new IllegalArgumentException("User not found")
        );

        // 2. CREATE CALENDAR ENTITY
        Calendar calendar = calendarMapper.toCalendar(request);
        calendar.setOwnerId(userId);

        // 3. CREATE OWNER MEMBER ENTITY
        Member member = memberMapper.toMember(userResponse);
        member.setRole(Role.OWNER);

        // 4. ESTABLISH RELATIONSHIPS
        member.setCalendar(calendar);
        calendar.getMembers().add(member);

        // 5. PERSIST TO DATABASE
        calendarRepository.save(calendar);
        memberRepository.save(member);

        return null;
    }

    public List<CalendarResponse> searchPublicCalendars(String name) {
        List<Calendar> calendars;

        if (name == null || name.isEmpty()) {
            // Return all public calendars when no name provided
            calendars = calendarRepository.findByVisibility(Visibility.PUBLIC);
        } else {
            // Return public calendars with names starting with the provided prefix
            calendars = calendarRepository.findByVisibilityAndNameStartingWithIgnoreCase(Visibility.PUBLIC, name);
        }

        return calendars.stream()
                .map(calendarMapper::fromCalendar)
                .toList();
    }
}
