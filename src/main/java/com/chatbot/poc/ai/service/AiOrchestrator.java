package com.chatbot.poc.ai.service;

import com.chatbot.poc.conversation.domain.ConversationSession;

public interface AiOrchestrator {
    String chat(ConversationSession session, String userMessage);
}
