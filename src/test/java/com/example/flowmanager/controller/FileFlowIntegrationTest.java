package com.example.flowmanager.controller;

import com.example.flowmanager.BaseIntegrationTest;
import com.example.flowmanager.dto.FileStatusResponse;
import com.example.flowmanager.dto.UploadFileResponse;
import com.example.flowmanager.messaging.ConversionStatus;
import com.example.flowmanager.messaging.ConvertedEvent;
import com.example.flowmanager.model.FileTask;
import com.example.flowmanager.model.OutboxMessage;
import com.example.flowmanager.model.OutboxStatus;
import com.example.flowmanager.model.TaskStatus;
import com.example.flowmanager.repository.FileTaskRepository;
import com.example.flowmanager.repository.OutboxRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("File Flow Integration Tests")
class FileFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FileTaskRepository fileTaskRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MinioClient minioClient;

    @Test
    @DisplayName("Upload file -> complete conversion -> download PDF successfully")
    void shouldExecuteFullFileFlowSuccessfully() throws Exception {
        String username = "integrationTestUser";

        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test_document.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello World Enterprise Content".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(mockFile)
                        .header("X-User-Login", username))
                .andExpect(status().isOk())
                .andReturn();

        String uploadResponseStr = uploadResult.getResponse().getContentAsString();
        UploadFileResponse uploadResponse = objectMapper.readValue(uploadResponseStr, UploadFileResponse.class);
        UUID taskId = uploadResponse.taskId();

        assertThat(taskId).isNotNull();

        Optional<FileTask> initialTaskOpt = fileTaskRepository.findById(taskId);
        assertThat(initialTaskOpt).isPresent();
        assertThat(initialTaskOpt.get().getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

        MvcResult statusResultBefore = mockMvc.perform(get("/api/v1/files/" + taskId + "/status")
                        .header("X-User-Login", username))
                .andExpect(status().isOk())
                .andReturn();
        FileStatusResponse statusResponseBefore = objectMapper.readValue(statusResultBefore.getResponse().getContentAsString(), FileStatusResponse.class);
        assertThat(statusResponseBefore.status()).isEqualTo(TaskStatus.IN_PROGRESS);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<OutboxMessage> allMessages = outboxRepository.findAll();
            boolean hasProcessed = allMessages.stream()
                    .anyMatch(msg -> msg.getStatus() == OutboxStatus.PROCESSED);
            assertThat(hasProcessed).isTrue();
        });

        String dummyConvertedPath = "conversions/converted/" + taskId + ".pdf";
        byte[] dummyPdfContent = "%PDF-1.5 fake pdf content".getBytes();

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("conversions")
                        .object("converted/" + taskId + ".pdf")
                        .stream(new ByteArrayInputStream(dummyPdfContent), dummyPdfContent.length, -1)
                        .contentType(MediaType.APPLICATION_PDF_VALUE)
                        .build()
        );

        ConvertedEvent conversionResultEvent = new ConvertedEvent(
                taskId,
                ConversionStatus.SUCCESS,
                dummyConvertedPath,
                null
        );
        String kafkaPayload = objectMapper.writeValueAsString(conversionResultEvent);

        kafkaTemplate.send("file-conversion-results", taskId.toString(), kafkaPayload).get(5, TimeUnit.SECONDS);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<FileTask> updatedTaskOpt = fileTaskRepository.findById(taskId);
            assertThat(updatedTaskOpt).isPresent();
            assertThat(updatedTaskOpt.get().getStatus()).isEqualTo(TaskStatus.SUCCESS);
        });

        MvcResult downloadResultActions = mockMvc.perform(get("/api/v1/files/" + taskId + "/download")
                        .header("X-User-Login", username))
                .andReturn();

        MvcResult finalDownloadResult = mockMvc.perform(asyncDispatch(downloadResultActions))
                .andExpect(status().isOk())
                .andReturn();

        byte[] downloadedContent = finalDownloadResult.getResponse().getContentAsByteArray();

        assertThat(finalDownloadResult.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(finalDownloadResult.getResponse().getHeader("Content-Disposition"))
                .contains("attachment; filename=\"converted_" + taskId + ".pdf\"");

        assertThat(downloadedContent).isEqualTo(dummyPdfContent);
    }
}