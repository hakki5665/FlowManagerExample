package com.example.flowmanager.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "subscription-service", path = "/api/v1/subscriptions")
public interface SubscriptionClient {

    @GetMapping("/{username}")
    SubscriptionDto getSubscription(@PathVariable("username") String username);
}