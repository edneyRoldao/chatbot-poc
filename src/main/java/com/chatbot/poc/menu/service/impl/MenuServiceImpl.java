package com.chatbot.poc.menu.service.impl;

import com.chatbot.poc.menu.domain.MenuItem;
import com.chatbot.poc.menu.domain.PizzaSize;
import com.chatbot.poc.menu.service.MenuService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
class MenuServiceImpl implements MenuService {

    private static final List<MenuItem> MENU_ITEMS = List.of(
            new MenuItem("Margherita", "Molho de tomate, mussarela, manjericão fresco",
                    Map.of(PizzaSize.SMALL, new BigDecimal("29.00"),
                            PizzaSize.MEDIUM, new BigDecimal("39.00"),
                            PizzaSize.LARGE, new BigDecimal("49.00"))),
            new MenuItem("Calabresa", "Linguiça calabresa, mussarela, cebola",
                    Map.of(PizzaSize.SMALL, new BigDecimal("32.00"),
                            PizzaSize.MEDIUM, new BigDecimal("42.00"),
                            PizzaSize.LARGE, new BigDecimal("52.00"))),
            new MenuItem("Frango com Catupiry", "Frango desfiado, catupiry, mussarela",
                    Map.of(PizzaSize.SMALL, new BigDecimal("35.00"),
                            PizzaSize.MEDIUM, new BigDecimal("45.00"),
                            PizzaSize.LARGE, new BigDecimal("55.00"))),
            new MenuItem("Pepperoni", "Pepperoni importado, mussarela, azeitona",
                    Map.of(PizzaSize.SMALL, new BigDecimal("38.00"),
                            PizzaSize.MEDIUM, new BigDecimal("48.00"),
                            PizzaSize.LARGE, new BigDecimal("58.00"))),
            new MenuItem("Portuguesa", "Presunto, ovos, cebola, mussarela, azeitona",
                    Map.of(PizzaSize.SMALL, new BigDecimal("36.00"),
                            PizzaSize.MEDIUM, new BigDecimal("46.00"),
                            PizzaSize.LARGE, new BigDecimal("56.00")))
    );

    @Override
    public String getMenuAsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("🍕 Cardápio da Pizzaria do João\n\n");
        sb.append("PIZZAS DISPONÍVEIS:\n");
        for (MenuItem item : MENU_ITEMS) {
            sb.append("• ").append(item.name())
                    .append(" — ").append(item.description()).append("\n");
            sb.append("  Pequena (4 fatias): ").append(formatPrice(item.priceFor(PizzaSize.SMALL)));
            sb.append(" | Média (6 fatias): ").append(formatPrice(item.priceFor(PizzaSize.MEDIUM)));
            sb.append(" | Grande (8 fatias): ").append(formatPrice(item.priceFor(PizzaSize.LARGE)));
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public Optional<MenuItem> findItem(String name, PizzaSize size) {
        return MENU_ITEMS.stream()
                .filter(item -> item.name().equalsIgnoreCase(name))
                .findFirst();
    }

    private String formatPrice(BigDecimal price) {
        return String.format("R$ %.2f", price).replace('.', ',');
    }
}
