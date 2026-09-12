package com.example.flowmanager.dto;

import com.example.flowmanager.model.TaskStatus;

import java.util.UUID;

public record FileStatusResponse(
        UUID taskId,
        TaskStatus status,
        String convertedPath
) {}