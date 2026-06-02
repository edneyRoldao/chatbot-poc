# Spec: Messaging Strategy

## Goal

Decouple the conversation orchestrator from any specific messaging platform.
Adding a new provider (e.g., WhatsApp, Instagram) requires zero changes to existing code.

## Interface Contract

```
MessagingProvider
├── send(OutboundMessage) → void
└── providerName() → String
```

## Inbound Flow

```
Platform (Telegram/WhatsApp)
  └─► Provider Entry Point (polling or webhook)
        └─► builds InboundMessage
              └─► ConversationRouter.route(InboundMessage)
                    └─► ConversationService.handle(InboundMessage)
                          └─► AiOrchestrator.chat(sessionId, text)
                                └─► MessagingProviderRegistry.get(providerName)
                                      └─► MessagingProvider.send(OutboundMessage)
```

## Outbound Flow

```
ConversationService (or AiOrchestrator)
  └─► builds OutboundMessage(recipientId, text)
        └─► MessagingProviderRegistry.get("telegram")
              └─► TelegramMessagingProvider.send(OutboundMessage)
```

## Session ID Namespacing

Session IDs are namespaced by provider to avoid cross-provider collisions:
- `1234567890@telegram`
- `5511999999999@whatsapp`

The same user on different platforms gets different sessions (intentional for MVP).

## Provider Registration

All `MessagingProvider` implementations are Spring `@Component` beans.
`MessagingProviderRegistry` receives them as `List<MessagingProvider>` via constructor injection
and builds a `Map<String, MessagingProvider>` keyed by `providerName()`.

No configuration file needed to register a new provider — Spring wires it automatically.

## Phase 1 Implementation (MVP)

- [x] `MessagingProvider` interface
- [x] `InboundMessage` record
- [x] `OutboundMessage` record
- [x] `MessagingProviderRegistry` component
- [ ] `TelegramMessagingProvider` (Phase 2)
- [ ] `WhatsAppMessagingProvider` (future)

## Constraints

- `MessagingProvider.send()` must never throw — catch all exceptions internally and log
- Message text is always plain string — no HTML/Markdown at this layer
- Formatting is provider-specific and applied inside each provider implementation
