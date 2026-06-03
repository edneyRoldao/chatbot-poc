package com.chatbot.poc.conversation.service.impl;

import com.chatbot.poc.ai.service.AiOrchestrator;
import com.chatbot.poc.conversation.domain.ConversationSession;
import com.chatbot.poc.conversation.repository.ConversationRepository;
import com.chatbot.poc.conversation.service.ConversationService;
import com.chatbot.poc.messaging.domain.InboundMessage;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final AiOrchestrator aiOrchestrator;

    ConversationServiceImpl(ConversationRepository conversationRepository, AiOrchestrator aiOrchestrator) {
        this.conversationRepository = conversationRepository;
        this.aiOrchestrator = aiOrchestrator;
    }

    @Override
    public String handle(InboundMessage message) {
        ConversationSession session = loadOrCreateSession(message);
        String reply = aiOrchestrator.chat(session, message.text());
        session.setUpdatedAt(Instant.now());
        conversationRepository.save(session);
        return reply;
    }

    private ConversationSession loadOrCreateSession(InboundMessage message) {
        return conversationRepository.findById(message.sessionId())
                .orElseGet(() -> ConversationSession.newSession(
                        message.sessionId(), message.providerName(), message.senderId()));
    }
}
