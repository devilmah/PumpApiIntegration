package com.example.kafka_test.integration.model;

public class Notification {
    private final String rawMessage;

    public Notification(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getRawMessage() {
        return rawMessage;
    }
}