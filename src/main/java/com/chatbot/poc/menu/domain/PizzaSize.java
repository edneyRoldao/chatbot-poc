package com.chatbot.poc.menu.domain;

public enum PizzaSize {
    SMALL("Pequena", 4),
    MEDIUM("Média", 6),
    LARGE("Grande", 8);

    private final String displayName;
    private final int slices;

    PizzaSize(String displayName, int slices) {
        this.displayName = displayName;
        this.slices = slices;
    }

    public String displayName() { return displayName; }
    public int slices() { return slices; }
}
