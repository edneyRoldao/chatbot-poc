package com.chatbot.poc.order.repository;

import com.chatbot.poc.order.domain.Order;
import com.chatbot.poc.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findTopBySessionIdAndStatusOrderByCreatedAtDesc(String sessionId, OrderStatus status);
}
