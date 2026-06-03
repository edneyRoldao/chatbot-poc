# Chatbot POC — Flow & Architecture

## Request flow (Telegram → AI → Telegram)

```
Telegram (user sends message)
  └─▶ TelegramBotConfig.onUpdateReceived()
        builds InboundMessage { sessionId="chatId@telegram", senderId, text }
  └─▶ ConversationRouter.route()
        calls ConversationService.handle()
  └─▶ ConversationServiceImpl.handle()
        1. loads ConversationSession from Redis (or creates new one)
        2. calls AiOrchestrator.chat(session, userText)
        3. saves updated session to Redis
        returns reply text
  └─▶ ConversationRouter.sendReply()
        looks up TelegramMessagingProvider by name
        sends OutboundMessage back to the user
```

## How the AI decides what to respond

`GroqAiOrchestrator` uses **LangChain4j AiServices** to call Groq (llama-3.3-70b-versatile)
via the OpenAI-compatible API. Each call does:

1. Reconstructs a `ChatMemory` from `session.history` (up to `max-history-turns * 2` messages).
2. Builds a `PizzeriaAssistant` (AiServices) with that memory and `PizzeriaTools`.
3. Calls `assistant.chat(userText)` — LangChain4j handles the tool-call loop internally:
   the model may call `getMenu()`, `placeOrder()`, or `cancelOrder()` before returning the final text.
4. Syncs the updated message list back to `session.history`.

The **system prompt** (`prompts/system-prompt.txt`) is the brain:
it contains the persona, rules, full menu, expected flow, and example responses.
The model follows it to decide tone, order collection, confirmation, etc.

### Tools available to the AI

| Tool | What it does |
|------|-------------|
| `getMenu()` | Returns the menu text (built from `MenuServiceImpl`) |
| `placeOrder(name, size, qty)` | Generates a fake order number — no DB persistence |
| `cancelOrder()` | Returns a cancellation message |

## What is rigid right now (and why)

### 1. Menu duplicated in two places
The menu exists verbatim in `system-prompt.txt` **and** in `MenuServiceImpl`.
If a pizza price changes, both need updating independently — they will drift.
`getMenu()` was added to feed the dynamic menu to the model, but since Groq/Llama
ignores tool results for display purposes (it won't paste the text itself),
the system prompt carries a static copy as a workaround.

### 2. `ConversationState` is unused
The enum `IDLE / GREETING / BROWSING / ORDERING / CONFIRMING / ORDER_PLACED` exists
in `ConversationSession` but is never read or written after `newSession()`.
The AI manages all conversational state implicitly through message history.
The enum is dead code.

### 3. `currentOrderDraft` is unused
Also declared in `ConversationSession` but never populated.
The AI collects order data in its own context; there is no structured draft on the Java side.

### 4. `placeOrder` is a stub
No database write. The order number is `System.currentTimeMillis() % 100000`.
There is no Order entity, no persistence, no way to look up a placed order.

### 5. `PizzeriaAssistant` interface is hardwired to the pizzeria
The `@SystemMessage(fromResource = "prompts/system-prompt.txt")` annotation is
burned into a private interface inside `GroqAiOrchestrator`.
Changing the persona (e.g. pharmacy bot) means changing Java code, not just config.

## Session storage

Sessions live in Redis, keyed by `sessionId` (`chatId@telegram`).
TTL is `app.conversation.session-ttl-minutes` (default 30 min).
History is a plain `List<Map<String, String>>` serialized as JSON — `role` + `content` pairs.

## Configuration quick reference

```yaml
app:
  ai:
    model: llama-3.3-70b-versatile   # Groq model
    max-tokens: 1024
    temperature: 0.7
    max-history-turns: 20            # pairs kept in ChatMemory
  conversation:
    session-ttl-minutes: 30
  messaging:
    provider: telegram
```
