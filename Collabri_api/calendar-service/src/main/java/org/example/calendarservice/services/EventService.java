package org.example.calendarservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.calendarservice.dto.EventRequest;
import org.example.calendarservice.dto.EventResponse;
import org.example.calendarservice.entites.Event;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.exceptions.CustomException;
import org.example.calendarservice.kafka.EventCreatedEvent;
import org.example.calendarservice.kafka.InviteProducer;
import org.example.calendarservice.mappers.EventMapper;
import org.example.calendarservice.repositories.CalendarRepository;
import org.example.calendarservice.repositories.EventRepository;
import org.example.calendarservice.repositories.MemberRepository;
import org.example.calendarservice.user.UserClient;
import org.example.calendarservice.user.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CalendarRepository calendarRepository;
    private final InviteProducer inviteProducer;
    private final UserClient userClient;
    private final MemberRepository memberRepository;

    //-------------------------------- Publish Event Notification ---------------------------------//
    public void publishEventNotification(UUID eventId, String title, String createdBy, String calendarName, String location, UUID calendarId) {
        var eventCreatedEvent = new EventCreatedEvent(
                eventId,
                title,
                createdBy,
                calendarName,
                location,
                memberRepository.findAllByCalendarId(calendarId).stream()
                        .map(Member::getEmail)
                        .toList()
        );
        inviteProducer.sendEventCreatedNotification(eventCreatedEvent);
        log.info("Published event created notification for event {}", eventId);

    }

    //-------------------------------- Event Services ---------------------------------//

    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#calendarId, authentication, 'MANAGER')")
    @Transactional
    public void createEvent(EventRequest request, UUID calendarId, Authentication authentication) {
        String userIdStr = authentication.getName();
        UUID userId = UUID.fromString(userIdStr);
        Event event = eventMapper.toEvent(request);
        event.setCreatedBy(userId);
        event.setCalendar(calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CustomException("Calendar not found", HttpStatus.NOT_FOUND)));
        eventRepository.save(event);

        //-------------------------------- Publish Event Created Notification ---------------------------------//
        UserResponse user = userClient.findUserbyId(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        publishEventNotification(
                event.getId(),
                event.getTitle(),
                user.email(),
                event.getCalendar().getName(),
                event.getLocation(),
                calendarId

        );
        log.info("Created event {} for calendar {}", event.getId(), calendarId);

    }

    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#calendarId, authentication, 'VIEWER')")
    public List<EventResponse> getEventsByCalendar(UUID calendarId) {
        return eventRepository.findAllByCalendarId(calendarId).stream()
                .map(eventMapper::fromEvent)
                .toList();
    }

    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#calendarId, authentication, 'MANAGER')")
    @Transactional
    public void updateEvent(EventRequest request, UUID eventId, UUID calendarId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException("Event not found", HttpStatus.NOT_FOUND));
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setLocation(request.location());
        eventRepository.save(event);
        log.info("Updated event {}", eventId);
    }

    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#calendarId, authentication, 'OWNER')")
    public void deleteEvent(UUID eventId, UUID calendarId) {
        eventRepository.deleteById(eventId);
        log.info("Deleted event {}", eventId);
    }
}