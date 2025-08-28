package org.example.calendarservice.services;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.enums.Visibility;
import org.example.calendarservice.repositories.CalendarRepository;
import org.example.calendarservice.repositories.MemberRepository;
import org.example.calendarservice.user.UserClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final MemberMapper mapper;
    private final CalendarRepository calendarRepository;
    private final UserClient userClient;

    public void joinPublicCalendar(UUID calendarId, Authentication authentication) {
        Calendar calendar = calendarRepository.findById(calendarId).orElseThrow(
                () -> new IllegalArgumentException("calendar not found")
        );

        if (calendar.getVisibility() != Visibility.PUBLIC) {
            throw new SecurityException("calendar is not public");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaim("userId").toString());

        if (memberRepository.existsByUserIdAndCalendarId(userId, calendarId)) {
            throw new IllegalStateException("User is already a member of this calendar");
        }

        var userResponse = userClient.findUserbyId(userId).orElseThrow(() ->
                new IllegalArgumentException("User not found")
        );

        Member member = mapper.toMember(userResponse);
        member.setCalendar(calendar);
        memberRepository.save(member);  // Save member first
        calendar.getMembers().add(member);
        calendarRepository.save(calendar);
    }
}
