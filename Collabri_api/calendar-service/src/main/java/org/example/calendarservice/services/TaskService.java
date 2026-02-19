package org.example.calendarservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.calendarservice.dto.TaskRequest;
import org.example.calendarservice.dto.TaskResponse;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.entites.Task;
import org.example.calendarservice.exceptions.CustomException;
import org.example.calendarservice.kafka.InviteProducer;
import org.example.calendarservice.kafka.TaskCreatedEvent;
import org.example.calendarservice.mappers.TaskMapper;
import org.example.calendarservice.repositories.CalendarRepository;
import org.example.calendarservice.repositories.MemberRepository;
import org.example.calendarservice.repositories.TaskRepository;
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
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final CalendarRepository calendarRepository;
    private final MemberRepository memberRepository;
    private final InviteProducer inviteProducer;

    //-------------------------------- Publish Task Notification ---------------------------------//
    // (Placeholder for future task notification methods)
    public void publishTaskNotification(UUID taskId, String title, String assignTo, String createdBy, String calendarName, UUID recipipentId) {
        // Implementation for publishing task notifications will go here
        var taskCreatedEvent = new TaskCreatedEvent(
                taskId,
                title,
                assignTo,
                createdBy,
                calendarName,
                recipipentId
        ); // Placeholder
        inviteProducer.sendTaskCreatedNotification(taskCreatedEvent);
        log.info("Published task created notification for task {}", taskId);
    }


    //-------------------------------- Task Services ---------------------------------//
    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#calendarId, authentication, 'MANAGER')")
    @Transactional
    public void createTask(TaskRequest request, UUID calendarId, Authentication authentication) {
        String userIdStr = authentication.getName();
        UUID userId = UUID.fromString(userIdStr);
        Task task = taskMapper.toTask(request);
        Member assignedTo = memberRepository.findById(request.assignedTo())
                .orElseThrow(() -> new CustomException("Assigned member not found", HttpStatus.NOT_FOUND));
        task.setCalendar(calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CustomException("Calendar not found", HttpStatus.NOT_FOUND)));
        task.setAssignedTo(assignedTo);
        task.setCreatedBy(userId);
        taskRepository.save(task);
        log.info("Created task {} for calendar {}", task.getId(), calendarId);

        String senderEmail = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND))
                .getEmail();
        //-------------------------------- Publish Task Created Notification ---------------------------------//
        publishTaskNotification(
                task.getId(),
                task.getTitle(),
                assignedTo.getEmail(),
                senderEmail,
                task.getCalendar().getName(),
                assignedTo.getUserId()
        );
        log.info("Published task created notification for task {}", task.getId());
    }

    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#calendarId, authentication, 'VIEWER')")
    public List<TaskResponse> getTasksByCalendar(UUID calendarId) {
        return taskRepository.findAllByCalendarId(calendarId).stream()
                .map(taskMapper::fromTask)
                .toList();
    }

    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#calendarId, authentication, 'MANAGER')")
    @Transactional
    public void updateTask(TaskRequest request, UUID taskId, UUID calendarId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException("Task not found", HttpStatus.NOT_FOUND));
        Member assignedTo = memberRepository.findById(request.assignedTo())
                .orElseThrow(() -> new CustomException("Assigned member not found", HttpStatus.NOT_FOUND));
        task.setAssignedTo(assignedTo);
        task.setTaskStatus(request.taskStatus());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        taskRepository.save(task);
        log.info("Updated task {}", taskId);
    }

    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#calendarId, authentication, 'OWNER')")
    public void deleteTask(UUID taskId, UUID calendarId) {
        taskRepository.deleteById(taskId);
        log.info("Deleted task {}", taskId);
    }
}