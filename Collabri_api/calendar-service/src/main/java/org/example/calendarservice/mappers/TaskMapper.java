package org.example.calendarservice.mappers;

import org.example.calendarservice.dto.TaskRequest;
import org.example.calendarservice.dto.TaskResponse;
import org.example.calendarservice.entites.Task;
import org.springframework.stereotype.Service;

@Service
public class TaskMapper {

    public Task toTask(TaskRequest request) {
        return Task.builder()
                .title(request.title())
                .description(request.description())
                .taskStatus(request.taskStatus())
                .dueDate(request.dueDate())
                .build();
    }

    public TaskResponse fromTask(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getCalendar().getId(),
                task.getAssignedTo().getId(),
                task.getDueDate(),
                task.getTaskStatus(),
                task.getCreatedAt()
        );
    }
}