# Telegram Bot Integration

## Library

`org.telegram:telegrambots:6.9.7` (core, no Spring Boot auto-configuration)

## Bot Setup

1. Create bot via [@BotFather](https://t.me/BotFather) on Telegram
2. Get `BOT_TOKEN` and `BOT_USERNAME`
3. Set them in `.env` (local) or env vars (production)

## Long Polling Mode (Dev)

The bot polls Telegram API continuously. No public URL required for dev.

```java
@Component
public class TelegramBotPoller extends TelegramLongPollingBot {

    private final ConversationRouter conversationRouter;

    public TelegramBotPoller(
        @Value("${app.telegram.bot-token}") String botToken,
        ConversationRouter conversationRouter
    ) {
        super(botToken);
        this.conversationRouter = conversationRouter;
    }

    @Override
    public String getBotUsername() {
        return botUsername; // from @Value
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
```

## Bot Registration

Register the bot with `TelegramBotsApi` on application startup:

```java
@Component
public class TelegramBotRegistrar implements ApplicationRunner {

    private final TelegramBotPoller bot;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
    }
}
```

## Sending Messages

Implement `MessagingProvider` for outbound messages:

```java
@Component
public class TelegramMessagingProvider implements MessagingProvider {

    private final TelegramBotPoller bot;

    @Override
    public void send(OutboundMessage message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.recipientId());
        sendMessage.setText(message.text());
        sendMessage.enableMarkdown(true);
        execute(sendMessage); // via bot.execute()
    }

    @Override
    public String providerName() {
        return "telegram";
    }
}
```

## Session ID Format

`{chatId}@telegram` — e.g., `1234567890@telegram`

Chat ID is unique per user per bot. Use it as the conversation session key in Redis.

## Message Length Limit

Telegram has a 4096-character limit per message. If the AI response exceeds this,
split by paragraph (`\n\n`) and send as multiple messages.

## Markdown Support

Telegram supports MarkdownV2. Keep AI responses simple (bold, line breaks) to avoid
escaping issues. Prefer plain text for MVP.

## Webhook Mode (Production)

For production, switch from polling to webhook:
1. Set `TELEGRAM_WEBHOOK_URL` env var (must be HTTPS)
2. Register webhook: `POST https://api.telegram.org/bot{token}/setWebhook?url={url}`
3. Expose `POST /webhook/telegram` endpoint
4. Verify secret token in header `X-Telegram-Bot-Api-Secret-Token`
