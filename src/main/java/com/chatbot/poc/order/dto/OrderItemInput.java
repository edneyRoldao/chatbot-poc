package com.chatbot.poc.order.dto;

public record OrderItemInput(
        String pizzaName,
        String pizzaSize,
        int quantity
) {}
