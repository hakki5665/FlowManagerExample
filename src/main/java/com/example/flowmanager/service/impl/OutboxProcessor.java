package com.example.flowmanager.service.impl;

import com.example.flowmanager.model.OutboxMessage;
import com.example.flowmanager.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void processOutboxMessages() {
        List<OutboxMessage> pendingMessages = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING");

        if (pendingMessages.isEmpty()) {
            return;
        }

        log.info("Найдено {} сообщений в Outbox для отправки", pendingMessages.size());

        for (OutboxMessage message : pendingMessages) {
            sendAndCommit(message);
        }
    }

    @Transactional
    public void sendAndCommit(OutboxMessage message) {
        try {
            SendResult<String, String> result = kafkaTemplate.send(
                    message.getTopic(),
                    message.getId().toString(),
                    message.getPayload()
            ).get(5, TimeUnit.SECONDS);

            message.setStatus("PROCESSED");
            outboxRepository.save(message);
            log.debug("Сообщение {} успешно доставлено в топик {}", message.getId(), message.getTopic());

        } catch (Exception e) {
            log.error("Критическая ошибка при обработке или доставке outbox сообщения {}", message.getId(), e);

            message.setStatus("FAILED");
            outboxRepository.save(message);
        }
    }
}