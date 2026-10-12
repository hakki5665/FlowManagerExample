package com.example.flowmanager.client;

import java.io.Serializable;

public class SubscriptionDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String subscriptionType;

    public SubscriptionDto() {}

    public SubscriptionDto(String username, String subscriptionType) {
        this.username = username;
        this.subscriptionType = subscriptionType;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSubscriptionType() { return subscriptionType; }
    public void setSubscriptionType(String subscriptionType) { this.subscriptionType = subscriptionType; }
}