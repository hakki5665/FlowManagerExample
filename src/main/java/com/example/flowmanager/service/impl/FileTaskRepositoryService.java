package com.example.flowmanager.service.impl;

import com.example.flowmanager.model.FileTask;
import com.example.flowmanager.model.OutboxMessage;
import com.example.flowmanager.model.OutboxStatus;
import com.example.flowmanager.model.TaskStatus;
import com.example.flowmanager.repository.FileTaskRepository;
import com.example.flowmanager.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileTaskRepositoryService {

    private final FileTaskRepository taskRepository;
    private final OutboxRepository outboxRepository;

    @Transactional
    public void saveTaskAndOutbox(UUID taskId, String storagePath, String jsonPayload) {
        FileTask task = FileTask.builder()
                .id(taskId)
                .originalPath(storagePath)
                .status(TaskStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();
        taskRepository.save(task);

        OutboxMessage outboxMessage = OutboxMessage.builder()
                .topic("file-conversion-requests")
                .payload(jsonPayload)
                .status(OutboxStatus.PENDING) // Используем Enum
                .createdAt(LocalDateTime.now())
                .build();
        outboxRepository.save(outboxMessage);
    }
}