# Messaging Strategy Pattern

## Overview

All messaging providers (Telegram, WhatsApp, etc.) implement `MessagingProvider`.
The orchestrator never depends on a concrete provider — only on the interface.

## Core Interface

```java
public interface MessagingProvider {
    void send(OutboundMessage message);
    String providerName();
}
```

## Domain Types

```java
// Inbound — received from any provider
public record InboundMessage(
    String sessionId,     // unique conversation identifier (e.g., chatId@telegram)
    String senderId,      // platform-specific sender ID
    String text,          // message text content
    String providerName   // which provider received this message
) {}

// Outbound — sent via any provider
public record OutboundMessage(
    String recipientId,   // platform-specific recipient ID
    String text           // message text to send
) {}
```

## Provider Naming Convention

Each provider identifies itself with a unique name string:
- `"telegram"`
- `"whatsapp"`

This name is used for routing and logging. It must be consistent across all usages.

## Provider Registry

A `MessagingProviderRegistry` holds all available providers and routes by name:

```java
@Component
public class MessagingProviderRegistry {

    private final Map<String, MessagingProvider> providers;

    public MessagingProviderRegistry(List<MessagingProvider> providerList) {
        this.providers = providerList.stream()
            .collect(Collectors.toMap(MessagingProvider::providerName, Function.identity()));
    }

    public MessagingProvider get(String providerName) {
        MessagingProvider provider = providers.get(providerName);
        if (provider == null) throw new IllegalArgumentException("Unknown provider: " + providerName);
        return provider;
    }
}
```

## Telegram Implementation Notes

- Extends `TelegramLongPollingBot` from `org.telegram:telegrambots`
- Registered as a Spring `@Component`
- `onUpdateReceived(Update update)` is the entry point for incoming messages
- Session ID format: `{chatId}@telegram`
- Delegates to `ConversationService` after building `InboundMessage`

## WhatsApp Implementation Notes (future)

- Receives messages via webhook POST
- Session ID format: `{phoneNumber}@whatsapp`
- Validates HMAC signature before processing
- Same `MessagingProvider` interface, different `@Component`

## Adding a New Provider

1. Create `{Provider}MessagingProvider.java` in `messaging/service/impl/`
2. Implement `MessagingProvider` interface
3. Annotate with `@Component`
4. The registry picks it up automatically via Spring's `List<MessagingProvider>` injection
5. No other code changes required — zero modification of existing providers

## Session ID Convention

Session IDs are scoped per provider to avoid collisions:
`{platformId}@{providerName}` — e.g., `1234567890@telegram`, `5511999999999@whatsapp`
