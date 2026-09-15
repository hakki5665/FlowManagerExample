package com.example.flowmanager.service.impl;

import com.example.flowmanager.dto.FileStatusResponse;
import com.example.flowmanager.dto.UploadFileResponse;
import com.example.flowmanager.model.FileTask;
import com.example.flowmanager.model.TaskStatus;
import com.example.flowmanager.messaging.ToConvertEvent;
import com.example.flowmanager.messaging.ConvertedEvent;
import com.example.flowmanager.repository.FileTaskRepository;
import com.example.flowmanager.service.core.IFileFlowService;
import com.example.flowmanager.service.core.IStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileFlowService implements IFileFlowService {

    private final IStorageService storageService;
    private final FileTaskRepository taskRepository;
    private final ObjectMapper objectMapper;
    private final FileTaskRepositoryService taskRepositoryService;

    @Override
    @SneakyThrows
    public UploadFileResponse initFlow(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        UUID taskId = UUID.randomUUID();
        String storagePath = storageService.upload(file, taskId.toString());

        String pureFileName = storagePath.substring(storagePath.indexOf("/") + 1);
        ToConvertEvent event = new ToConvertEvent(taskId, pureFileName);
        String jsonPayload = objectMapper.writeValueAsString(event);

        taskRepositoryService.saveTaskAndOutbox(taskId, storagePath, jsonPayload);

        return new UploadFileResponse(taskId, "Файл успешно загружен и принят в обработку");
    }

    @Override
    @Transactional(readOnly = true)
    public FileStatusResponse getStatus(UUID taskId) {
        FileTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Задача не найдена"));

        return new FileStatusResponse(task.getId(), task.getStatus(), task.getConvertedPath());
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream downloadConvertedFile(UUID taskId) {
        FileTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Задача с ID " + taskId + " не найдена"));

        if (task.getStatus() != TaskStatus.SUCCESS) {
            throw new IllegalStateException("Файл еще не готов или произошла ошибка. Текущий статус: " + task.getStatus());
        }

        if (task.getConvertedPath() == null || task.getConvertedPath().isBlank()) {
            throw new IllegalStateException("Путь к сконвертированному файлу отсутствует в БД");
        }

        return storageService.download(task.getConvertedPath());
    }

    @Override
    @Transactional
    public void completeFlow(ConvertedEvent event) {
        FileTask task = taskRepository.findById(event.requestId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Задача с ID " + event.requestId() + " не найдена в системе. Отправка в DLT топик."));

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            log.info("Задача {} уже обработана.", task.getId());
            return;
        }

        if ("SUCCESS".equalsIgnoreCase(event.status())) {
            task.setStatus(TaskStatus.SUCCESS);
            task.setConvertedPath(event.convertedFilePath());
            log.info("Задача {} успешно завершена.", task.getId());
        } else {
            task.setStatus(TaskStatus.ERROR);
            log.error("Задача {} завершилась ошибкой. Причина: {}", task.getId(), event.errorMessage());
        }

        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
    }
}