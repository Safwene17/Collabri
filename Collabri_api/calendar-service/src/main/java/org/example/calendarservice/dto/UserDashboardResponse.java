package org.example.calendarservice.dto;

public record UserDashboardResponse(
        long ownedCalendars,
        long joinedCalendars,
        long totalCalendars,
        long publicCalendars,
        long privateCalendars,
        long totalEventsCreated,
        long upcomingEvents,
        long totalTasksAssigned,
        long completedTasks,
        long pendingTasks,
        long calendarsAsOwner,
        long calendarsAsManager,
        long calendarsAsViewer,
        long pendingInvites
) {
}

