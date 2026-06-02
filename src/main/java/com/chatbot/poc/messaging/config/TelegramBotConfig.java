package com.chatbot.poc.messaging.config;

import com.chatbot.poc.messaging.component.ConversationRouter;
import com.chatbot.poc.messaging.domain.InboundMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class TelegramBotConfig extends TelegramLongPollingBot {

    private final String botUsername;
    private final ConversationRouter conversationRouter;

    public TelegramBotConfig(
        @Value("${app.telegram.bot-token}") String botToken,
        @Value("${app.telegram.bot-username}") String botUsername,
        @Lazy ConversationRouter conversationRouter
    ) {
        super(botToken);
        this.botUsername = botUsername;
        this.conversationRouter = conversationRouter;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!hasTextMessage(update)) return;
        InboundMessage message = buildInboundMessage(update);
        conversationRouter.route(message);
    }

    private boolean hasTextMessage(Update update) {
        return update.hasMessage() && update.getMessage().hasText();
    }

    private InboundMessage buildInboundMessage(Update update) {
        Message msg = update.getMessage();
        String sessionId = msg.getChatId() + "@telegram";
        return new InboundMessage(sessionId, String.valueOf(msg.getChatId()), msg.getText(), "telegram");
    }

}
