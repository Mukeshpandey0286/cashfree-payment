package com.rental.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CashfreeConfig {

    @Value("${cashfree.app-id}")
    private String appId;

    @Value("${cashfree.secret-key}")
    private String secretKey;

    @Value("${cashfree.base-url}")
    private String baseUrl;

    @Value("${cashfree.webhook-secret}")
    private String webhookSecret;

    // Getters
    public String getAppId() {
        return appId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }
}