package org.example.calendarservice.services;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.enums.Visibility;
import org.example.calendarservice.repositories.CalendarRepository;
import org.example.calendarservice.repositories.MemberRepository;
import org.example.calendarservice.user.UserClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final MemberMapper mapper;
    private final CalendarRepository calendarRepository;
    private final UserClient userClient;

    public void joinPublicCalendar(UUID calendarId, UUID userId) {
        Calendar calendar = calendarRepository.findById(calendarId).orElseThrow(
                () -> new IllegalArgumentException("calendar not found")
        );
        if (calendar.getVisibility() != Visibility.PUBLIC) {
            throw new SecurityException("calendar is not public");
        }
        if (memberRepository.existsByUserIdAndCalendarId(userId, calendarId)) {
            throw new IllegalStateException("User is already a member of this calendar");
        }
        var userResponse = userClient.findUserbyId(userId).orElseThrow();
        Member member = mapper.toMember(userResponse);
        member.setCalendar(calendar);
        memberRepository.save(member);  // Save member first
        calendar.getMembers().add(member);
        calendarRepository.save(calendar);
    }
}
