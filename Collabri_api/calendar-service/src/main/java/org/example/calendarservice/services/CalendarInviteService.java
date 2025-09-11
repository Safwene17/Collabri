package org.example.calendarservice.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.calendarservice.entites.Calendar;
import org.example.calendarservice.entites.CalendarInvite;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.enums.InviteStatus;
import org.example.calendarservice.enums.Role;
import org.example.calendarservice.kafka.CalendarInviteEvent;
import org.example.calendarservice.kafka.InviteProducer;
import org.example.calendarservice.repositories.CalendarInviteRepository;
import org.example.calendarservice.repositories.CalendarRepository;
import org.example.calendarservice.repositories.MemberRepository;
import org.example.calendarservice.user.UserClient;
import org.example.calendarservice.utils.TokenUtil; // your utility
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarInviteService {

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
        String inviterEmail = userClient.findUserbyId(callerUserId).map(u -> u.email()).orElse(defaultFromAddress);
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

    // ---------------------------
    // Public API
    // ---------------------------

    /**
     * Create or reuse an invite. Returns the plaintext token (development/testing only).
     * In production you should NOT return the plaintext token via API responses.
     */
    @Transactional
    public String inviteMember(UUID calendarId, String destinationEmailRaw, Authentication authentication) {

        if (destinationEmailRaw == null || destinationEmailRaw.isBlank()) {
            throw new IllegalArgumentException("destinationEmail required");
        }
        String destinationEmail = destinationEmailRaw.trim().toLowerCase();

        // 1) validate calendar exists
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("Calendar not found"));

        // 2) authentication + caller id
        if (authentication == null) throw new SecurityException("Authentication required to invite");
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID callerUserId = UUID.fromString(jwt.getClaim("userId").toString());

        // 3) permission check (owner or manager)
        boolean isOwner = calendar.getOwnerId().equals(callerUserId);
        boolean isManager = memberRepository.findByUserIdAndCalendarId(callerUserId, calendarId)
                .map(m -> m.getRole() == Role.MANAGER || m.getRole() == Role.OWNER)
                .orElse(false);

        if (!isOwner && !isManager) throw new SecurityException("Only owner or managers can send invites");

        // 4) member check (already member)
        if (memberRepository.existsByCalendarIdAndEmailIgnoreCase(calendarId, destinationEmail)) {
            throw new IllegalArgumentException("User with email " + destinationEmail + " is already a member of this calendar");
        }

        // 5) find existing invite for this email (any status)
        Optional<CalendarInvite> existingOpt = calendarInviteRepository.findByCalendarIdAndDestinationEmailIgnoreCase(calendarId, destinationEmail);

        if (existingOpt.isPresent()) {
            CalendarInvite inv = existingOpt.get();

            // expire if needed
            expireIfNeeded(inv);

            // handle statuses
            if (inv.getStatus() == InviteStatus.PENDING) {
                log.info("Pending invite already exists for {} on calendar {}", destinationEmail, calendarId);
                // We can't return the plaintext token because we only store hash.
                // Return null (idempotent). If you need the token in dev, log it manually when created.
                return null;
            }

            if (inv.getStatus() == InviteStatus.ACCEPTED) {
                log.info("Invite already accepted for {} on calendar {}", destinationEmail, calendarId);
                return null;
            }

            // For EXPIRED or CANCELLED: rotate token (generate new plain token -> hash -> save -> publish)
            String newPlainToken = TokenUtil.generatePlainToken();
            String newHash = TokenUtil.hashTokenSha256Hex(newPlainToken);

            inv.setTokenHash(newHash);
            inv.setExpiresAt(Instant.now().plus(inviteExpireDays, ChronoUnit.DAYS));
            inv.setStatus(InviteStatus.PENDING);
            inv.setInvitedByUserId(callerUserId);
            calendarInviteRepository.save(inv);

            publishInviteEvent(inv, calendar, callerUserId, destinationEmail, newPlainToken);
            return newPlainToken;
        }

        // 6) create new invite row
        String plainToken = TokenUtil.generatePlainToken();
        String tokenHash = TokenUtil.hashTokenSha256Hex(plainToken);
        Instant expiresAt = Instant.now().plus(inviteExpireDays, ChronoUnit.DAYS);

        CalendarInvite invite = CalendarInvite.builder()
                .calendarId(calendarId)
                .destinationEmail(destinationEmail)
                .invitedByUserId(callerUserId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .status(InviteStatus.PENDING)
                .build();

        calendarInviteRepository.save(invite);

        publishInviteEvent(invite, calendar, callerUserId, destinationEmail, plainToken);

        log.info("Invite created for {} to calendar {} by {}", destinationEmail, calendarId, callerUserId);

        // return plaintext token for dev/testing only — remove or change in production
        return plainToken;
    }

    // Accept with authenticated user (incoming token is plaintext)
    @Transactional
    public void acceptInviteWithAuth(String plainToken, Authentication authentication) {
        if (authentication == null) throw new SecurityException("Authentication required");

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaim("userId").toString());

        String tokenHash = TokenUtil.hashTokenSha256Hex(plainToken);
        CalendarInvite invite = calendarInviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));

        expireIfNeeded(invite);
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new IllegalStateException("Invite is not pending: " + invite.getStatus());
        }

        // Verify destination email matches authenticated user (if invite had email)
        var userResp = userClient.findUserbyId(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (invite.getDestinationEmail() != null && !invite.getDestinationEmail().equalsIgnoreCase(userResp.email())) {
            throw new SecurityException("Invite email does not match authenticated user");
        }

        UUID calId = invite.getCalendarId();
        if (memberRepository.existsByUserIdAndCalendarId(userId, calId)) {
            invite.setStatus(InviteStatus.ACCEPTED);
            invite.setTokenHash(null); // clear token to make single-use
            calendarInviteRepository.save(invite);
            return;
        }

        Member member = memberMapper.toMember(userResp);
        member.setUserId(userId);
        member.setCalendar(calendarRepository.findById(calId).orElseThrow(() -> new IllegalArgumentException("Calendar not found")));
        memberRepository.save(member);

        invite.setStatus(InviteStatus.ACCEPTED);
        invite.setTokenHash(null); // single-use
        calendarInviteRepository.save(invite);

        log.info("User {} accepted invite to calendar {}", userId, calId);
    }


    // Decline (supports authenticated or token)
    @Transactional
    public void declineInvite(String plainToken, Authentication authentication) {
        String tokenHash = TokenUtil.hashTokenSha256Hex(plainToken);
        CalendarInvite invite = calendarInviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));

        expireIfNeeded(invite);
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new IllegalStateException("Invite is not pending: " + invite.getStatus());
        }

        if (authentication != null) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            UUID userId = UUID.fromString(jwt.getClaim("userId").toString());
            var userResp = userClient.findUserbyId(userId).orElseThrow();
            if (invite.getDestinationEmail() != null && !invite.getDestinationEmail().equalsIgnoreCase(userResp.email())) {
                throw new SecurityException("Invite email does not match authenticated user");
            }
        }

        invite.setStatus(InviteStatus.CANCELLED);
        invite.setTokenHash(null); // single-use
        calendarInviteRepository.save(invite);

        log.info("Invite {} declined", invite.getId());
    }
}
