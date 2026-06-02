package com.chatbot.poc.ai.service.impl;

import com.chatbot.poc.ai.component.PizzeriaTools;
import com.chatbot.poc.ai.service.AiOrchestrator;
import com.chatbot.poc.conversation.domain.ConversationSession;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
class GroqAiOrchestrator implements AiOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(GroqAiOrchestrator.class);

    private final OpenAiChatModel chatModel;
    private final PizzeriaTools pizzeriaTools;
    private final int maxHistoryTurns;

    GroqAiOrchestrator(
            OpenAiChatModel chatModel,
            PizzeriaTools pizzeriaTools,
            @Value("${app.ai.max-history-turns}") int maxHistoryTurns) {
        this.chatModel = chatModel;
        this.pizzeriaTools = pizzeriaTools;
        this.maxHistoryTurns = maxHistoryTurns;
    }

    @Override
    public String chat(ConversationSession session, String userMessage) {
        try {
            ChatMemory memory = buildMemoryForSession(session);
            PizzeriaAssistant assistant = buildAssistant(memory);
            String response = assistant.chat(userMessage);
            updateSessionHistory(session, memory.messages());
            return response;
        } catch (Exception e) {
            log.error("AI call failed for session [{}]", session.getSessionId(), e);
            return "Desculpe, estou com dificuldades técnicas no momento. Tente novamente em instantes.";
        }
    }

    private ChatMemory buildMemoryForSession(ConversationSession session) {
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(maxHistoryTurns * 2);
        for (ChatMessage message : toMessages(session.getHistory())) {
            memory.add(message);
        }
        return memory;
    }

    private PizzeriaAssistant buildAssistant(ChatMemory memory) {
        return AiServices.builder(PizzeriaAssistant.class)
                .chatModel(chatModel)
                .chatMemory(memory)
                .tools(pizzeriaTools)
                .build();
    }

    private List<ChatMessage> toMessages(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<ChatMessage> messages = new ArrayList<>();
        for (Map<String, String> entry : history) {
            String role = entry.get("role");
            String content = entry.get("content");
            if (StringUtils.isBlank(content)) {
                continue;
            }
            if ("user".equals(role)) {
                messages.add(UserMessage.from(content));
            } else if ("assistant".equals(role)) {
                messages.add(AiMessage.from(content));
            }
        }
        return messages;
    }

    private void updateSessionHistory(ConversationSession session, List<ChatMessage> messages) {
        List<Map<String, String>> history = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message instanceof UserMessage userMsg) {
                String content = extractText(userMsg);
                if (StringUtils.isNotBlank(content)) {
                    Map<String, String> entry = new HashMap<>();
                    entry.put("role", "user");
                    entry.put("content", content);
                    history.add(entry);
                }
            } else if (message instanceof AiMessage aiMsg && StringUtils.isNotBlank(aiMsg.text())) {
                Map<String, String> entry = new HashMap<>();
                entry.put("role", "assistant");
                entry.put("content", aiMsg.text());
                history.add(entry);
            }
        }
        session.setHistory(history);
    }

    private String extractText(UserMessage userMessage) {
        return userMessage.singleText();
    }

    private interface PizzeriaAssistant {
        @SystemMessage(fromResource = "prompts/system-prompt.txt")
        String chat(String userMessage);
    }
}
