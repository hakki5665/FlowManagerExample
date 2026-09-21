package com.example.flowmanager.dto;

import java.util.UUID;

public record UploadFileResponse(
        UUID taskId,
        String message
) {}