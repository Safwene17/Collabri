// file: src/main/java/org/example/calendarservice/services/MemberService.java
package org.example.calendarservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.calendarservice.dto.MemberResponse;
import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.enums.Role;
import org.example.calendarservice.enums.Visibility;
import org.example.calendarservice.exceptions.CustomException;
import org.example.calendarservice.kafka.InviteProducer;
import org.example.calendarservice.kafka.MemberJoinedEvent;
import org.example.calendarservice.kafka.MemberLeftEvent;
import org.example.calendarservice.mappers.MemberMapper;
import org.example.calendarservice.repositories.CalendarRepository;
import org.example.calendarservice.repositories.MemberRepository;
import org.example.calendarservice.user.UserClient;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper mapper;
    private final CalendarRepository calendarRepository;
    private final UserClient userClient;
    private final InviteProducer inviteProducer;


    @PreAuthorize("@verified.isVerified(authentication)")
    @Transactional
    public void joinPublicCalendar(UUID calendarId, Authentication authentication) {
        String userIdStr = authentication.getName();
        UUID userId = UUID.fromString(userIdStr);

        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CustomException("Calendar not found", HttpStatus.NOT_FOUND));

        if (calendar.getVisibility() != Visibility.PUBLIC) {
            throw new CustomException("Calendar is not public", HttpStatus.FORBIDDEN);
        }

        if (memberRepository.existsByUserIdAndCalendarId(userId, calendarId)) {
            throw new CustomException("User is already a member", HttpStatus.CONFLICT);
        }

        var userResponse = userClient.findUserbyId(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        Member member = mapper.toMember(userResponse);
        member.setRole(Role.VIEWER);  // Fixed: Set default role for new members
        calendar.addMember(member);
        log.info("User {} joined calendar {}", userId, calendarId);

        //--- Future: Publish Member Activity Event ---
        var memberJoinedEvent = new MemberJoinedEvent(
                member.getDisplayName(),
                member.getCalendar().getId(),
                member.getCalendar().getName(),
                memberRepository.findAllByRoleIn(List.of(Role.OWNER, Role.MANAGER)).stream()
                        .map(Member::getUserId)
                        .toList()
        );
        inviteProducer.sendMemberJoinedNotification(memberJoinedEvent);
    }

    // Java
//    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#calendarId, authentication, 'VIEWER')")
    public List<MemberResponse> getCalendarMembers(UUID calendarId) {
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CustomException("Calendar not found", HttpStatus.NOT_FOUND));
        if (calendar.getVisibility() == Visibility.PUBLIC) {
            return memberRepository.findAllByCalendarId(calendarId).stream()
                    .map(mapper::fromMember)
                    .collect(Collectors.toList());
        }
        throw new CustomException("Calendar is not public", HttpStatus.FORBIDDEN);
    }

    //    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#calendarId, authentication, 'MANAGER')")
    public MemberResponse getMemberById(UUID memberId, UUID calendarId) {
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CustomException("Calendar not found", HttpStatus.NOT_FOUND));
        if (calendar.getVisibility() == Visibility.PUBLIC) {
            return memberRepository.findById(memberId)
                    .map(mapper::fromMember)
                    .orElseThrow(() -> new CustomException("Member not found", HttpStatus.NOT_FOUND));
        }
        throw new CustomException("Calendar is not public", HttpStatus.FORBIDDEN);
    }


    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#calendarId, authentication, 'OWNER')")
    @Transactional
    public void removeMember(UUID memberId, UUID calendarId) {
        if (!memberRepository.existsByIdAndCalendarId(memberId, calendarId)) {
            throw new CustomException("Member not found in calendar", HttpStatus.NOT_FOUND);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException("Member not found", HttpStatus.NOT_FOUND));
        memberRepository.deleteById(memberId);

        var memberLeftEvent = new MemberLeftEvent(
                member.getDisplayName(),
                member.getCalendar().getId(),
                member.getCalendar().getName(),
                memberRepository.findAllByRoleIn(List.of(Role.OWNER, Role.MANAGER)).stream()
                        .map(Member::getUserId)
                        .toList()
        );
        inviteProducer.sendMemberLeftNotification(memberLeftEvent);
    }

    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.isOwner(#calendarId, authentication)")
    @Transactional
    public void setMemberRole(UUID memberId, UUID calendarId, Role newRole) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException("Member not found", HttpStatus.NOT_FOUND));
        if (!member.getCalendar().getId().equals(calendarId)) {
            throw new CustomException("Member not in this calendar", HttpStatus.NOT_FOUND);
        }
        member.setRole(newRole);
        log.info("Set role {} for member {} in calendar {}", newRole, memberId, calendarId);
    }
}