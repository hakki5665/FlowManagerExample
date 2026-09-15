package com.example.flowmanager.service.core;

import com.example.flowmanager.dto.FileStatusResponse;
import com.example.flowmanager.dto.UploadFileResponse;
import com.example.flowmanager.messaging.ConvertedEvent;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

public interface IFileFlowService {
    UploadFileResponse initFlow(MultipartFile file);

    FileStatusResponse getStatus(UUID taskId);

    void completeFlow(ConvertedEvent event);

    InputStream downloadConvertedFile(UUID taskId);
}