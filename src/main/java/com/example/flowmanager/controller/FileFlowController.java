package com.example.flowmanager.controller;

import com.example.flowmanager.dto.FileStatusResponse;
import com.example.flowmanager.dto.UploadFileResponse;
import com.example.flowmanager.service.core.IFileFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileFlowController {

    private final IFileFlowService fileFlowService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadFileResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        UploadFileResponse response = fileFlowService.initFlow(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{taskId}/status")
    public ResponseEntity<FileStatusResponse> getStatus(@PathVariable UUID taskId) {
        FileStatusResponse response = fileFlowService.getStatus(taskId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{taskId}/download")
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable UUID taskId) {
        InputStream fileStream = fileFlowService.downloadConvertedFile(taskId);

        StreamingResponseBody responseBody = outputStream -> {
            byte[] buffer = new byte[8192];
            int bytesRead;
            try (fileStream) { // Автозакрытие потока ввода
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        };

        String fileName = "converted_" + taskId + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(responseBody);
    }
}