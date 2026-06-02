package com.chatbot.poc.messaging.service.impl;

import com.chatbot.poc.messaging.config.TelegramBotConfig;
import com.chatbot.poc.messaging.domain.OutboundMessage;
import com.chatbot.poc.messaging.service.MessagingProvider;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramMessagingProvider implements MessagingProvider {

    private final TelegramBotConfig bot;

    public TelegramMessagingProvider(TelegramBotConfig bot) {
        this.bot = bot;
    }

    @Override
    public void send(OutboundMessage message) {
        SendMessage sendMessage = buildSendMessage(message);
        executeSendMessageOrFail(sendMessage, message.recipientId());
    }

    @Override
    public String providerName() {
        return "telegram";
    }

    private SendMessage buildSendMessage(OutboundMessage message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.recipientId());
        sendMessage.setText(message.text());
        return sendMessage;
    }

    private void executeSendMessageOrFail(SendMessage sendMessage, String recipientId) {
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Failed to send message to recipient: " + recipientId, e);
        }
    }

}
