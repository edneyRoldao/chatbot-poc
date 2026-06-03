package com.chatbot.poc.shared.component;

import com.chatbot.poc.conversation.domain.ConversationSession;
import org.springframework.stereotype.Component;

@Component
public class ConversationContext {

    private static final ThreadLocal<ConversationSession> holder = new ThreadLocal<>();

    public void set(ConversationSession session) {
        holder.set(session);
    }

    public ConversationSession get() {
        return holder.get();
    }

    public void clear() {
        holder.remove();
    }
}
