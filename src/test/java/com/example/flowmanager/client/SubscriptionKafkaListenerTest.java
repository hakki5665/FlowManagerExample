package com.example.flowmanager.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Subscription Kafka Listener Unit Tests")
class SubscriptionKafkaListenerTest {

    @Mock
    private SubscriptionCacheService cacheService;

    @InjectMocks
    private SubscriptionKafkaListener kafkaListener;

    @Test
    @DisplayName("Receive Kafka message -> trigger Redis cache invalidation successfully")
    void shouldTriggerCacheInvalidationWhenKafkaMessageReceived() {
        String username = "testuser";

        kafkaListener.listenSubscriptionInvalidation(username);

        verify(cacheService, times(1)).invalidateCache("testuser");
    }

    @Test
    @DisplayName("Receive empty Kafka message -> ignore and do not trigger invalidation")
    void shouldIgnoreInvalidationWhenUsernameIsEmpty() {
        kafkaListener.listenSubscriptionInvalidation("   ");

        verifyNoInteractions(cacheService);
    }
}