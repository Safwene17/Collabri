package org.example.calendarservice.repositories;

import org.example.calendarservice.entites.Task;
import org.example.calendarservice.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findAllByCalendarId(UUID calendarId);

    long countByAssignedToUserId(UUID userId);

    long countByAssignedToUserIdAndTaskStatusIn(UUID userId, List<TaskStatus> statuses);
}