package com.example.flowmanager.service.impl;

import com.example.flowmanager.model.OutboxMessage;
import com.example.flowmanager.model.OutboxStatus;
import com.example.flowmanager.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxTransportService {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    public void sendAndCommit(OutboxMessage message) {
        UUID messageId = message.getId();

        try {
            transactionTemplate.executeWithoutResult(status -> {
                OutboxMessage msg = outboxRepository.findById(messageId)
                        .orElseThrow(() -> new IllegalArgumentException("Сообщение не найдено: " + messageId));

                msg.setStatus(OutboxStatus.PROCESSED);
                outboxRepository.save(msg);
            });

            kafkaTemplate.send(
                    message.getTopic(),
                    messageId.toString(),
                    message.getPayload()
            ).get(5, TimeUnit.SECONDS);

            log.info("Сообщение Outbox {} успешно закоммичено в БД и отправлено в Kafka", messageId);

        } catch (Exception e) {
            log.error("Ошибка при обработке outbox сообщения {}. Откат статуса.", messageId, e);

            try {
                transactionTemplate.executeWithoutResult(status -> {
                    outboxRepository.findById(messageId).ifPresent(msg -> {
                        msg.setStatus(OutboxStatus.PENDING);
                        outboxRepository.save(msg);
                    });
                });
            } catch (Exception dbEx) {
                log.error("Не удалось вернуть статус в PENDING для сообщения {}", messageId, dbEx);
            }
        }
    }
}