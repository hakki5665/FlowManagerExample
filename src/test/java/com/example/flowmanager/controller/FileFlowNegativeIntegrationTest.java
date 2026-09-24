package com.example.flowmanager.controller;

import com.example.flowmanager.BaseIntegrationTest;
import com.example.flowmanager.dto.UploadFileResponse;
import com.example.flowmanager.messaging.ConversionStatus;
import com.example.flowmanager.messaging.ConvertedEvent;
import com.example.flowmanager.model.FileTask;
import com.example.flowmanager.model.TaskStatus;
import com.example.flowmanager.repository.FileTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("File Flow Negative Scenarios")
class FileFlowNegativeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FileTaskRepository fileTaskRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("Upload empty file -> return 400 Bad Request")
    void shouldReturnBadRequestWhenFileIsEmpty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.txt", MediaType.TEXT_PLAIN_VALUE, new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/files/upload").file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Conversion fails -> set task status to ERROR")
    void shouldSetStatusToErrorWhenConversionFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes()
        );
        MvcResult result = mockMvc.perform(multipart("/api/v1/files/upload").file(file)).andReturn();
        UUID taskId = objectMapper.readValue(result.getResponse().getContentAsString(), UploadFileResponse.class).taskId();

        ConvertedEvent failedEvent = new ConvertedEvent(
                taskId,
                ConversionStatus.FAILED,
                null,
                "Converter crashed: Corrupted file structure"
        );

        Thread.sleep(2000);
        kafkaTemplate.send("file-conversion-results", taskId.toString(), objectMapper.writeValueAsString(failedEvent)).get();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<FileTask> taskOpt = fileTaskRepository.findById(taskId);
            assertThat(taskOpt).isPresent();
            assertThat(taskOpt.get().getStatus()).isEqualTo(TaskStatus.ERROR);
        });

        mockMvc.perform(get("/api/v1/files/" + taskId + "/download"))
                .andExpect(status().isBadRequest());
    }
}