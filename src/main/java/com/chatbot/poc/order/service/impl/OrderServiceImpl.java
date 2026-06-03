package com.chatbot.poc.order.service.impl;

import com.chatbot.poc.menu.domain.MenuItem;
import com.chatbot.poc.menu.domain.PizzaSize;
import com.chatbot.poc.menu.service.MenuService;
import com.chatbot.poc.order.domain.Order;
import com.chatbot.poc.order.domain.OrderItem;
import com.chatbot.poc.order.domain.OrderStatus;
import com.chatbot.poc.order.dto.OrderCreateRequest;
import com.chatbot.poc.order.dto.OrderItemInput;
import com.chatbot.poc.order.dto.OrderResponse;
import com.chatbot.poc.order.mapper.OrderMapper;
import com.chatbot.poc.order.repository.OrderRepository;
import com.chatbot.poc.order.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MenuService menuService;
    private final OrderMapper orderMapper;

    OrderServiceImpl(OrderRepository orderRepository, MenuService menuService, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.menuService = menuService;
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional
    public OrderResponse placeOrder(OrderCreateRequest request) {
        Order order = initializeOrder(request);
        populateItems(order, request);
        Order saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void cancelOrder(String sessionId) {
        orderRepository
                .findTopBySessionIdAndStatusOrderByCreatedAtDesc(sessionId, OrderStatus.CONFIRMED)
                .ifPresent(order -> {
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);
                });
    }

    private Order initializeOrder(OrderCreateRequest request) {
        Order order = new Order();
        order.setSessionId(request.sessionId());
        order.setProviderName(request.providerName());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(BigDecimal.ZERO);
        return order;
    }

    private void populateItems(Order order, OrderCreateRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemInput input : request.items()) {
            PizzaSize pizzaSize = parseSizeOrFail(input.pizzaSize());
            MenuItem menuItem = findMenuItemOrFail(input.pizzaName(), pizzaSize);
            BigDecimal unitPrice = menuItem.priceFor(pizzaSize);
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(input.quantity()));

            OrderItem item = buildOrderItem(order, input, pizzaSize, unitPrice, itemTotal);
            order.getItems().add(item);
            total = total.add(itemTotal);
        }
        order.setTotalAmount(total);
    }

    private PizzaSize parseSizeOrFail(String sizeStr) {
        try {
            return PizzaSize.valueOf(sizeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tamanho inválido: " + sizeStr + ". Use: SMALL, MEDIUM ou LARGE");
        }
    }

    private MenuItem findMenuItemOrFail(String pizzaName, PizzaSize size) {
        return menuService.findItem(pizzaName, size)
                .orElseThrow(() -> new IllegalArgumentException("Pizza não encontrada no cardápio: " + pizzaName));
    }

    private OrderItem buildOrderItem(Order order, OrderItemInput input, PizzaSize pizzaSize,
                                     BigDecimal unitPrice, BigDecimal totalPrice) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setPizzaName(input.pizzaName());
        item.setPizzaSize(pizzaSize.name());
        item.setQuantity(input.quantity());
        item.setUnitPrice(unitPrice);
        item.setTotalPrice(totalPrice);
        return item;
    }
}
