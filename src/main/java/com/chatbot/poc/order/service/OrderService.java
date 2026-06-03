package com.chatbot.poc.order.service;

import com.chatbot.poc.order.dto.OrderCreateRequest;
import com.chatbot.poc.order.dto.OrderResponse;

public interface OrderService {
    OrderResponse placeOrder(OrderCreateRequest request);
    void cancelOrder(String sessionId);
}
