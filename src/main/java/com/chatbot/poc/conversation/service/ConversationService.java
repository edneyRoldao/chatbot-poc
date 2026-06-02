package com.chatbot.poc.conversation.service;

import com.chatbot.poc.messaging.domain.InboundMessage;

public interface ConversationService {

    String handle(InboundMessage message);
}
