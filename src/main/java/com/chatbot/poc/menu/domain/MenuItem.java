package com.chatbot.poc.menu.domain;

import java.math.BigDecimal;
import java.util.Map;

public record MenuItem(
        String name,
        String description,
        Map<PizzaSize, BigDecimal> prices
) {
    public BigDecimal priceFor(PizzaSize size) {
        return prices.get(size);
    }
}
