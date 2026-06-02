package com.chatbot.poc.messaging.domain;

public record OutboundMessage(
    String recipientId,
    String text
) {}
