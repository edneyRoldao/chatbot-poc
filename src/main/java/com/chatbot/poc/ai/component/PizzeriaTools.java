package com.chatbot.poc.ai.component;

import com.chatbot.poc.conversation.domain.ConversationSession;
import com.chatbot.poc.conversation.domain.ConversationState;
import com.chatbot.poc.order.dto.OrderCreateRequest;
import com.chatbot.poc.order.dto.OrderItemInput;
import com.chatbot.poc.order.dto.OrderResponse;
import com.chatbot.poc.order.service.OrderService;
import com.chatbot.poc.shared.component.ConversationContext;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PizzeriaTools {

    private final OrderService orderService;
    private final ConversationContext conversationContext;

    PizzeriaTools(OrderService orderService, ConversationContext conversationContext) {
        this.orderService = orderService;
        this.conversationContext = conversationContext;
    }

    @Tool("Places a single order containing all items the customer confirmed. Always call this once with the complete list — never call it multiple times for the same order.")
    public String placeOrder(
            @P("All items in the order. Each item: pizzaName (exactly as in the menu), pizzaSize (SMALL, MEDIUM, or LARGE), quantity (plain integer, never a quoted string)") List<OrderItemInput> items
    ) {
        ConversationSession session = conversationContext.get();
        OrderCreateRequest request = new OrderCreateRequest(
                session.getSessionId(), session.getProviderName(), items);
        OrderResponse response = orderService.placeOrder(request);
        session.setState(ConversationState.ORDER_PLACED);
        return response.confirmationMessage();
    }

    @Tool("Cancels the current order in progress and resets the conversation")
    public String cancelOrder() {
        ConversationSession session = conversationContext.get();
        if (session != null) {
            orderService.cancelOrder(session.getSessionId());
        }
        return "Pedido cancelado com sucesso. Como mais posso te ajudar?";
    }
}
