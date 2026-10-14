package com.example.flowmanager.controller;

import com.example.flowmanager.BaseIntegrationTest;
import com.example.flowmanager.client.SubscriptionCacheService;
import com.example.flowmanager.client.SubscriptionDto;
import com.example.flowmanager.dto.UploadFileResponse;
import com.example.flowmanager.messaging.ConversionStatus;
import com.example.flowmanager.messaging.ConvertedEvent;
import com.example.flowmanager.model.FileTask;
import com.example.flowmanager.model.TaskStatus;
import com.example.flowmanager.repository.FileTaskRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
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

    @MockitoSpyBean
    private SubscriptionCacheService subscriptionCacheService;

    @MockitoSpyBean
    private MinioClient minioClient;

    @BeforeEach
    void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        doAnswer(invocation -> null).when(minioClient).putObject(any());
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
    @DisplayName("Upload large file (>100MB) with PREMIUM subscription -> successfully permitted")
    void shouldAllowLargeFileWhenUserHasPremiumSubscription() throws Exception {
        String username = "premiumUserIsolatedTestKey";
        SubscriptionDto mockSubscription = new SubscriptionDto(username, "PREMIUM");

        doReturn(mockSubscription).when(subscriptionCacheService).getSubscription(username);

        byte[] contentBytes = "Valid Enterprise File Content Data long content".getBytes();
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "enterprise_video.mp4", MediaType.MULTIPART_FORM_DATA_VALUE, contentBytes
        ) {
            @Override
            public long getSize() {
                return 105 * 1024 * 1024L;
            }
        };

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(largeFile)
                        .header("X-User-Login", username))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Upload large file (>100MB) with FREE subscription -> block and return 400 Bad Request")
    void shouldBlockLargeFileWhenUserHasFreeSubscription() throws Exception {
        String username = "freeUserIsolatedTestKey";
        SubscriptionDto mockSubscription = new SubscriptionDto(username, "FREE");

        doReturn(mockSubscription).when(subscriptionCacheService).getSubscription(username);

        byte[] contentBytes = "Valid Enterprise File Content Data long content".getBytes();
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "holiday_archive.zip", MediaType.MULTIPART_FORM_DATA_VALUE, contentBytes
        ) {
            @Override
            public long getSize() {
                return 105 * 1024 * 1024L;
            }
        };

        MvcResult result = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(largeFile)
                        .header("X-User-Login", username))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("Ошибка: Размер файла превышает 100 МБ. Для загрузки больших файлов оформите PREMIUM подписку.");
    }

    @Test
    @DisplayName("Upload exactly 100MB file with FREE subscription -> permitted successfully")
    void shouldAllowExactly100MBForFreeSubscription() throws Exception {
        String username = "borderUserIsolatedTestKey";
        SubscriptionDto mockSubscription = new SubscriptionDto(username, "FREE");

        doReturn(mockSubscription).when(subscriptionCacheService).getSubscription(username);

        byte[] contentBytes = "Valid Boundary File Content Data".getBytes();
        MockMultipartFile borderFile = new MockMultipartFile(
                "file", "exact_limit.zip", MediaType.MULTIPART_FORM_DATA_VALUE, contentBytes
        ) {
            @Override
            public long getSize() {
                return 100 * 1024 * 1024L;
            }
        };

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(borderFile)
                        .header("X-User-Login", username))
                .andExpect(status().isOk());
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
    }
}