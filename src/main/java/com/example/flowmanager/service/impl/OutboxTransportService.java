package com.example.flowmanager.service.impl;

import com.example.flowmanager.model.OutboxMessage;
import com.example.flowmanager.model.OutboxStatus;
import com.example.flowmanager.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxTransportService {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public void sendAndCommit(OutboxMessage message) {
        try {
            kafkaTemplate.send(
                    message.getTopic(),
                    message.getId().toString(),
                    message.getPayload()
            ).get(5, TimeUnit.SECONDS);

            message.setStatus(OutboxStatus.PROCESSED);
            outboxRepository.save(message);

        } catch (Exception e) {
            log.error("Критическая ошибка при обработке или доставке outbox сообщения {}", message.getId(), e);
            message.setStatus(OutboxStatus.FAILED);
            outboxRepository.save(message);
        }
    }
}