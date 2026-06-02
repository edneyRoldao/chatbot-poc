package com.chatbot.poc.conversation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConversationSession {

    private String sessionId;
    private ConversationState state;
    private String providerName;
    private String recipientId;
    private Map<String, Object> currentOrderDraft;
    private List<Map<String, String>> history;
    private Instant createdAt;
    private Instant updatedAt;

    public ConversationSession() {}

    public static ConversationSession newSession(String sessionId, String providerName, String recipientId) {
        ConversationSession session = new ConversationSession();
        session.sessionId = sessionId;
        session.state = ConversationState.IDLE;
        session.providerName = providerName;
        session.recipientId = recipientId;
        session.history = new ArrayList<>();
        Instant now = Instant.now();
        session.createdAt = now;
        session.updatedAt = now;
        return session;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public ConversationState getState() { return state; }
    public void setState(ConversationState state) { this.state = state; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public Map<String, Object> getCurrentOrderDraft() { return currentOrderDraft; }
    public void setCurrentOrderDraft(Map<String, Object> currentOrderDraft) { this.currentOrderDraft = currentOrderDraft; }

    public List<Map<String, String>> getHistory() { return history; }
    public void setHistory(List<Map<String, String>> history) { this.history = history; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
