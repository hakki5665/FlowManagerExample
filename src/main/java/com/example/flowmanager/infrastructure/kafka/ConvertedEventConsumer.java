package com.example.flowmanager.infrastructure.kafka;

import com.example.flowmanager.messaging.ConvertedEvent;
import com.example.flowmanager.service.core.IFileFlowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConvertedEventConsumer {

    private final IFileFlowService fileFlowService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "file-conversion-results",
            groupId = "flow-manager-final-stable-group"
    )
    public void consume(String rawJson) {
        try {
            ConvertedEvent event = objectMapper.readValue(rawJson, ConvertedEvent.class);

            fileFlowService.completeFlow(event);
            log.info("Успешно обработан ответ конвертера для задачи ID: {}", event.requestId());

        } catch (Exception e) {
            log.error("Ошибка при обработке сообщения конвертера: {}", e.getMessage());
        }
    }
}