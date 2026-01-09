package org.example.calendarservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.calendarservice.dto.TaskRequest;
import org.example.calendarservice.dto.TaskResponse;
import org.example.calendarservice.entites.Member;
import org.example.calendarservice.entites.Task;
import org.example.calendarservice.exceptions.CustomException;
import org.example.calendarservice.mappers.TaskMapper;
import org.example.calendarservice.repositories.CalendarRepository;
import org.example.calendarservice.repositories.MemberRepository;
import org.example.calendarservice.repositories.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("@verified.isVerified(authentication) and @ownershipChecker.hasAccess(#calendarId, authentication, 'MANAGER')")
    @Transactional
    public void createTask(TaskRequest request, UUID calendarId) {
        Task task = taskMapper.toTask(request);
        Member assignedTo = memberRepository.findById(request.assignedTo())
                .orElseThrow(() -> new CustomException("Assigned member not found", HttpStatus.NOT_FOUND));
        task.setCalendar(calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CustomException("Calendar not found", HttpStatus.NOT_FOUND)));
        task.setAssignedTo(assignedTo);
        taskRepository.save(task);
        log.info("Created task {} for calendar {}", task.getId(), calendarId);
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