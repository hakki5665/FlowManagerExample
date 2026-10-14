package com.example.flowmanager.client;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class SubscriptionCacheService {

    private static final String REDIS_KEY_PREFIX = "user:subscription:";
    private final SubscriptionClient subscriptionClient;
    private final RedisTemplate<String, Object> redisTemplate;

    public SubscriptionCacheService(SubscriptionClient subscriptionClient, RedisTemplate<String, Object> redisTemplate) {
        this.subscriptionClient = subscriptionClient;
        this.redisTemplate = redisTemplate;
    }

    public SubscriptionDto getSubscription(String username) {
        String key = REDIS_KEY_PREFIX + username;

        try {
            SubscriptionDto cached = (SubscriptionDto) redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return new SubscriptionDto(cached.getUsername(), cached.getSubscriptionType());
            }
        } catch (Exception e) {
            System.err.println("Ошибка чтения из Redis кэша: " + e.getMessage());
        }

        SubscriptionDto dto = subscriptionClient.getSubscription(username);

        try {
            SubscriptionDto dataToCache = new SubscriptionDto(dto.getUsername(), dto.getSubscriptionType());
            redisTemplate.opsForValue().set(key, dataToCache, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            System.err.println("Ошибка записи в Redis кэш: " + e.getMessage());
        }

        return dto;
    }

    public void invalidateCache(String username) {
        String key = REDIS_KEY_PREFIX + username;
        redisTemplate.delete(key);
        System.out.println("Кэш Redis для пользователя " + username + " успешно сброшен.");
    }
}