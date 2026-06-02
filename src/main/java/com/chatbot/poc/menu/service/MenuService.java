package com.chatbot.poc.menu.service;

import com.chatbot.poc.menu.domain.MenuItem;
import com.chatbot.poc.menu.domain.PizzaSize;

import java.util.Optional;

public interface MenuService {
    String getMenuAsText();
    Optional<MenuItem> findItem(String name, PizzaSize size);
}
