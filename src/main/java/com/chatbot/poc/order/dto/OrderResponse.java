package com.chatbot.poc.order.dto;

import com.chatbot.poc.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        String sessionId,
        OrderStatus status,
        BigDecimal totalAmount,
        String confirmationMessage
) {}
