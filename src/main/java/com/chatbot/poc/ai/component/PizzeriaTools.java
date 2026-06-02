package com.chatbot.poc.ai.component;

import com.chatbot.poc.menu.service.MenuService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PizzeriaTools {

    private static final Logger log = LoggerFactory.getLogger(PizzeriaTools.class);

    private final MenuService menuService;

    PizzeriaTools(MenuService menuService) {
        this.menuService = menuService;
    }

    @Tool("Retrieves the complete menu text. You MUST paste the entire returned text verbatim into your reply — the customer sees only your text, not the tool output.")
    public String getMenu() {
        log.info("Tool called: getMenu()");
        return "INCLUDE THIS ENTIRE TEXT VERBATIM IN YOUR RESPONSE TO THE CUSTOMER:\n\n"
                + menuService.getMenuAsText()
                + "\n\nAfter showing the menu, ask what they would like to order.";
    }

    @Tool("Places an order for the customer after their explicit confirmation")
    public String placeOrder(
            @P("Pizza name exactly as shown in the menu") String pizzaName,
            @P("Size: SMALL, MEDIUM, or LARGE") String size,
            @P("Quantity") int quantity
    ) {
        long orderNumber = System.currentTimeMillis() % 100000;
        return "Pedido #" + orderNumber + " registrado com sucesso! "
                + quantity + "x " + pizzaName + " (" + size + ") "
                + "chegará em 35 a 45 minutos. Obrigado por escolher a Pizzaria do João!";
    }

    @Tool("Cancels the current order in progress and resets the conversation")
    public String cancelOrder() {
        return "Pedido cancelado com sucesso. Como mais posso te ajudar?";
    }
}
