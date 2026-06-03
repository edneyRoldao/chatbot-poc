package com.chatbot.poc.order.dto;

import java.util.List;

public record OrderCreateRequest(
        String sessionId,
        String providerName,
        List<OrderItemInput> items
) {}
