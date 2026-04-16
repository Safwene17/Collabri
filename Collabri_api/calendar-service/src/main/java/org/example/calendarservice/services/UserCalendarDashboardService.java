package org.example.calendarservice.services;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.dto.UserDashboardResponse;
import org.example.calendarservice.enums.Role;
import org.example.calendarservice.enums.TaskStatus;
import org.example.calendarservice.enums.Visibility;
import org.example.calendarservice.repositories.CalendarRepository;
import org.example.calendarservice.repositories.EventRepository;
import org.example.calendarservice.repositories.MemberRepository;
import org.example.calendarservice.repositories.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCalendarDashboardService {

    private final CalendarRepository calendarRepository;
    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public UserDashboardResponse getDashboard(UUID userId) {
        long ownedCalendars = calendarRepository.countByOwnerId(userId);
        long joinedCalendars = memberRepository.countByUserId(userId);
        long publicCalendars = calendarRepository.countByOwnerIdAndVisibility(userId, Visibility.PUBLIC);
        long privateCalendars = calendarRepository.countByOwnerIdAndVisibility(userId, Visibility.PRIVATE);

        long totalEventsCreated = eventRepository.countCreatedByAccessible(userId);
        long upcomingEvents = eventRepository.countUpcomingAccessible(userId, LocalDateTime.now());

        long totalTasksAssigned = taskRepository.countByAssignedToUserId(userId);
        long completedTasks = taskRepository.countByAssignedToUserIdAndTaskStatusIn(userId, List.of(TaskStatus.COMPLETED));
        long pendingTasks = taskRepository.countByAssignedToUserIdAndTaskStatusIn(userId, List.of(TaskStatus.PENDING));

        long calendarsAsManager = memberRepository.countByUserIdAndRole(userId, Role.MANAGER);
        long calendarsAsViewer = memberRepository.countByUserIdAndRole(userId, Role.VIEWER);

        // TODO: Pending invites require cross-service email lookup (user-service) and are intentionally deferred.
        long pendingInvites = -1L;

        return new UserDashboardResponse(
                ownedCalendars,
                joinedCalendars,
                ownedCalendars + joinedCalendars,
                publicCalendars,
                privateCalendars,
                totalEventsCreated,
                upcomingEvents,
                totalTasksAssigned,
                completedTasks,
                pendingTasks,
                ownedCalendars,
                calendarsAsManager,
                calendarsAsViewer,
                pendingInvites
        );
    }
}

