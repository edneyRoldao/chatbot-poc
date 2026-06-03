package com.chatbot.poc.order.mapper;

import com.chatbot.poc.order.domain.Order;
import com.chatbot.poc.order.domain.OrderItem;
import com.chatbot.poc.order.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        String confirmationMessage = buildConfirmationMessage(order);
        return new OrderResponse(
                order.getId(),
                order.getSessionId(),
                order.getStatus(),
                order.getTotalAmount(),
                confirmationMessage
        );
    }

    private String buildConfirmationMessage(Order order) {
        String shortId = order.getId().toString().substring(0, 8).toUpperCase();
        String itemsSummary = order.getItems().stream()
                .map(this::formatItem)
                .collect(Collectors.joining(", "));
        return "Pedido #" + shortId + " confirmado: " + itemsSummary
                + ". Chegará em 35 a 45 minutos. Obrigado por escolher a Pizzaria do João!";
    }

    private String formatItem(OrderItem item) {
        return item.getQuantity() + "x " + item.getPizzaName() + " (" + item.getPizzaSize() + ")";
    }
}
