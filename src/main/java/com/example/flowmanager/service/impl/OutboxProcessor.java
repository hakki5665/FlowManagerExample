package com.example.flowmanager.service.impl;

import com.example.flowmanager.model.OutboxMessage;
import com.example.flowmanager.model.OutboxStatus;
import com.example.flowmanager.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final OutboxTransportService outboxTransportService;

    @Scheduled(fixedDelay = 1000)
    public void processOutboxMessages() {
        List<OutboxMessage> pendingMessages = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingMessages.isEmpty()) {
            return;
        }

        log.info("Найдено {} сообщений в Outbox для отправки", pendingMessages.size());

        for (OutboxMessage message : pendingMessages) {
            outboxTransportService.sendAndCommit(message);
        }
    }
}