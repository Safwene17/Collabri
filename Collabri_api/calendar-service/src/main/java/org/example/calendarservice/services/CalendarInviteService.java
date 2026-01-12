package org.example.calendarservice.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.calendarservice.config.VerifiedUserChecker;
import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.entites.CalendarInvite;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.enums.InviteStatus;
import org.example.calendarservice.exceptions.CustomException;
import org.example.calendarservice.kafka.CalendarInviteEvent;
import org.example.calendarservice.kafka.InviteProducer;
import org.example.calendarservice.mappers.MemberMapper;
import org.example.calendarservice.repositories.CalendarInviteRepository;
import org.example.calendarservice.repositories.CalendarRepository;
import org.example.calendarservice.repositories.MemberRepository;
import org.example.calendarservice.user.UserClient;
import org.example.calendarservice.user.UserResponse;
import org.example.calendarservice.utils.TokenUtil; // your utility
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarInviteService {

    private final VerifiedUserChecker verified;
    @Value("${frontend.invite.url:http://localhost:4200/invite-accept}")
    private String frontendInviteUrl;

    @Value("${app.invites.expire-days:7}")
    private int inviteExpireDays;

    @Value("${spring.mail.username:no-reply@example.com}")
    private String defaultFromAddress;

    private final InviteProducer inviteProducer;
    private final UserClient userClient;
    private final CalendarInviteRepository calendarInviteRepository;
    private final CalendarRepository calendarRepository;
    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    // ---------------------------
    // Helpers
    // ---------------------------
    private void expireIfNeeded(CalendarInvite invite) {
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(Instant.now())
                && invite.getStatus() == InviteStatus.PENDING) {
            invite.setStatus(InviteStatus.EXPIRED);
            invite.setTokenHash(null); // clear token for safety
            calendarInviteRepository.save(invite);
        }
    }

    private void publishInviteEvent(CalendarInvite invite, Calendar calendar, UUID callerUserId, String destinationEmail, String plainToken) {
        String inviterEmail = userClient.findUserbyId(callerUserId).map(UserResponse::email).orElse(defaultFromAddress);
        // publish plaintext token only to internal topic — notification service will send via email
        inviteProducer.sendCalendarInvitation(new CalendarInviteEvent(
                invite.getCalendarId(),
                calendar.getName(),
                inviterEmail,
                destinationEmail,
                plainToken,
                invite.getExpiresAt()
        ));
    }


    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#calendarId, authentication, 'MANAGER')")
    @Transactional
    public Void inviteMember(UUID calendarId, String destinationEmailRaw, Authentication authentication) {

        if (destinationEmailRaw == null || destinationEmailRaw.isBlank()) {
            throw new CustomException("destinationEmail required", HttpStatus.NOT_FOUND);
        }
        String destinationEmail = destinationEmailRaw.trim().toLowerCase();

        // 1) validate calendar exists
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CustomException("Calendar not found", HttpStatus.NOT_FOUND));


        // 4) member check (already member)
        if (memberRepository.existsByCalendarIdAndEmailIgnoreCase(calendarId, destinationEmail)) {
            throw new CustomException("User with email " + destinationEmail + " is already a member of this calendar", HttpStatus.CONFLICT);
        }
        String userIdStr = authentication.getName();
        UUID userId = UUID.fromString(userIdStr);

        // 5) find existing invite for this email (any status)
        Optional<CalendarInvite> existingOpt = calendarInviteRepository.findByCalendarIdAndDestinationEmailIgnoreCase(calendarId, destinationEmail);

        if (existingOpt.isPresent()) {
            CalendarInvite inv = existingOpt.get();

            // expire if needed
            expireIfNeeded(inv);

            // handle statuses
            if (inv.getStatus() == InviteStatus.PENDING) {
                throw new CustomException("Invite for this user already pending ", HttpStatus.CONFLICT);

            }

            if (inv.getStatus() == InviteStatus.ACCEPTED) {
                throw new CustomException("User already accepted the invitation", HttpStatus.OK);
            }

            // For EXPIRED or DECLINED: rotate token (generate new plain token -> hash -> save -> publish)
            String newPlainToken = TokenUtil.generatePlainToken();
            String newHash = TokenUtil.hashTokenSha256Hex(newPlainToken);

            inv.setTokenHash(newHash);
            inv.setExpiresAt(Instant.now().plus(inviteExpireDays, ChronoUnit.DAYS));
            inv.setStatus(InviteStatus.PENDING);
            inv.setInvitedByUserId(userId);
            calendarInviteRepository.save(inv);

            publishInviteEvent(inv, calendar, userId, destinationEmail, newPlainToken);
            return null;
        }


        // 6) create new invite row

        String plainToken = TokenUtil.generatePlainToken();
        String tokenHash = TokenUtil.hashTokenSha256Hex(plainToken);
        Instant expiresAt = Instant.now().plus(inviteExpireDays, ChronoUnit.DAYS);

        CalendarInvite invite = CalendarInvite.builder()
                .calendarId(calendarId)
                .destinationEmail(destinationEmail)
                .invitedByUserId(userId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .status(InviteStatus.PENDING)
                .build();

        calendarInviteRepository.save(invite);

        publishInviteEvent(invite, calendar, userId, destinationEmail, plainToken);

        log.info("Invite created for {} to calendar {} by {}", destinationEmail, calendarId, userId);

        return null;
    }

    // Accept with authenticated user (incoming token is plaintext)

    @PreAuthorize("@verified.isVerified(authentication)")
    @Transactional
    public Void acceptInviteWithAuth(String plainToken, Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaim("userId").toString());

        String tokenHash = TokenUtil.hashTokenSha256Hex(plainToken);
        CalendarInvite invite = calendarInviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new CustomException("Invite not found", HttpStatus.NOT_FOUND));

        expireIfNeeded(invite);
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new CustomException("Invite is not pending: " + invite.getStatus(), HttpStatus.CONFLICT);
        }

        // Verify destination email matches authenticated user (if invite had email)
        var userResp = userClient.findUserbyId(userId).orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        if (invite.getDestinationEmail() != null && !invite.getDestinationEmail().equalsIgnoreCase(userResp.email())) {
            throw new CustomException("Invite email does not match authenticated user", HttpStatus.CONFLICT);
        }

        UUID calId = invite.getCalendarId();
        if (memberRepository.existsByUserIdAndCalendarId(userId, calId)) {
            invite.setStatus(InviteStatus.ACCEPTED);
            calendarInviteRepository.save(invite);
            return null;
        }

        Member member = memberMapper.toMember(userResp);
        member.setUserId(userId);
        member.setCalendar(calendarRepository.findById(calId).orElseThrow(() -> new CustomException("Calendar not found", HttpStatus.NOT_FOUND)));
        memberRepository.save(member);

        invite.setStatus(InviteStatus.ACCEPTED);
        calendarInviteRepository.save(invite);

        log.info("User {} accepted invite to calendar {}", userId, calId);
        return null;
    }


    // Decline (supports authenticated or token)
    @Transactional
    @PreAuthorize("@verified.isVerified(authentication)")
    public Void declineInvite(String plainToken, Authentication authentication) {
        String tokenHash = TokenUtil.hashTokenSha256Hex(plainToken);
        CalendarInvite invite = calendarInviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new CustomException("Invite not found", HttpStatus.NOT_FOUND));

        expireIfNeeded(invite);
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new CustomException("Invite is not pending: " + invite.getStatus(), HttpStatus.CONFLICT);
        }

        if (authentication != null) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            UUID userId = UUID.fromString(jwt.getClaim("userId").toString());
            var userResp = userClient.findUserbyId(userId)
                    .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
            if (invite.getDestinationEmail() != null && !invite.getDestinationEmail().equalsIgnoreCase(userResp.email())) {
                throw new CustomException("Invite email does not match authenticated user", HttpStatus.CONFLICT);
            }
        }

        invite.setStatus(InviteStatus.DECLINED);
        calendarInviteRepository.save(invite);

        log.info("Invite {} declined", invite.getId());
        return null;
    }
}
