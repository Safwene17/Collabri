package org.example.calendarservice.mappers;

import lombok.extern.slf4j.Slf4j;
import org.example.calendarservice.dto.MemberResponse;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.enums.Role;
import org.example.calendarservice.user.UserResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MemberMapper {

    public Member toMember(UserResponse userResponse) {
        log.info(userResponse.toString());
        return Member.builder()
                .userId(userResponse.id())  // Store user ID as reference
                .displayName(userResponse.firstname() + " " + userResponse.lastname())
                .email(userResponse.email())
                .role(Role.VIEWER)
                .build();
    }

    public MemberResponse fromMember(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getUserId(),
                member.getDisplayName(),
                member.getEmail(),
                member.getRole()
        );

    }
}
