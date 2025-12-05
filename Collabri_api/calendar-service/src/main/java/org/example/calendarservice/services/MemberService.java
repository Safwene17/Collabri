package org.example.calendarservice.services;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.dto.MemberResponse;
import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.enums.Visibility;
import org.example.calendarservice.mappers.MemberMapper;
import org.example.calendarservice.repositories.CalendarRepository;
import org.example.calendarservice.repositories.MemberRepository;
import org.example.calendarservice.user.UserClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;
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
        memberRepository.save(member);
    }

    public Optional<List<Member>> getCalendarMembers(UUID calendarId, Authentication authentication) {
        Calendar calendar = calendarRepository.findById(calendarId).orElseThrow(
                () -> new IllegalArgumentException("calendar not found")
        );

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaim("userId").toString());

        if (!memberRepository.existsByUserIdAndCalendarId(userId, calendarId)) {
            throw new SecurityException("User is not a member of this calendar");
        }

        return memberRepository.findAllByCalendarId(calendarId);
    }

    public MemberResponse getMemberById(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("member not found")
        );
        return mapper.fromMember(member);
    }

    public void removeMember(Long memberId, Authentication authentication) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("member not found")
        );

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaim("userId").toString());

        if (!memberRepository.existsByUserIdAndCalendarId(userId, member.getCalendar().getId())) {
            throw new SecurityException("User is not a member of this calendar");
        }

        memberRepository.deleteById(memberId);
    }
}
