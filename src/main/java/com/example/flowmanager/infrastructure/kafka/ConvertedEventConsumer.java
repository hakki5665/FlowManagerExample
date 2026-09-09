package com.example.flowmanager.infrastructure.kafka;

import com.example.flowmanager.messaging.ConvertedEvent;
import com.example.flowmanager.service.core.IFileFlowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConvertedEventConsumer {

    private final IFileFlowService fileFlowService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "file-conversion-results", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, String> record) {
        log.info("Получено событие конвертации из Kafka с ключом: {}", record.key());

        try {
            ConvertedEvent event = objectMapper.readValue(record.value(), ConvertedEvent.class);

            fileFlowService.completeFlow(event);

        } catch (Exception e) {
            log.error("Критическая ошибка при обработке сообщения из топика converted: {}", record.value(), e);
        }
    }
}