package com.example.flowmanager.client;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class SubscriptionKafkaListener {

    private final SubscriptionCacheService cacheService;

    public SubscriptionKafkaListener(SubscriptionCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @KafkaListener(topics = "subscription-invalidations-topic", groupId = "flow-manager-subscription-group")
    public void listenSubscriptionInvalidation(String username) {
        if (username != null && !username.trim().isEmpty()) {
            cacheService.invalidateCache(username.trim());
        }
    }
}