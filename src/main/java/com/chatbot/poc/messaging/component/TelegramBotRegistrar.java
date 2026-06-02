package com.chatbot.poc.messaging.component;

import com.chatbot.poc.messaging.config.TelegramBotConfig;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class TelegramBotRegistrar implements ApplicationRunner {

    private final TelegramBotConfig bot;

    public TelegramBotRegistrar(TelegramBotConfig bot) {
        this.bot = bot;
    }

    @Override
    public void run(ApplicationArguments args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
    }
}
