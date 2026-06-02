package com.chatbot.poc.messaging.domain;

public record InboundMessage(
    String sessionId,
    String senderId,
    String text,
    String providerName
) {}
