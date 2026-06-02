package com.chatbot.poc.messaging.service;

import com.chatbot.poc.messaging.domain.OutboundMessage;

public interface MessagingProvider {

    void send(OutboundMessage message);

    String providerName();

}
