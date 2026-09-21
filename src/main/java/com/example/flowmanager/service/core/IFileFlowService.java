package com.example.flowmanager.service.core;

import com.example.flowmanager.dto.FileStatusResponse;
import com.example.flowmanager.dto.UploadFileResponse;
import com.example.flowmanager.messaging.ConvertedEvent;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.UUID;

public interface IFileFlowService {

    UploadFileResponse initFlow(MultipartFile file);

    FileStatusResponse getStatus(UUID taskId);

    StreamingResponseBody getConvertedFileStream(UUID taskId);

    void completeFlow(ConvertedEvent event);
}