# Spec: Conversation Flow

## Goal

Model the conversation as a state machine so the AI always operates within a defined context.
The state guides the system prompt injected into each LLM call.

## States

```
IDLE
  └─► GREETING          (first message received)
        └─► BROWSING     (user asks about menu or starts ordering)
              └─► ORDERING         (user selected a pizza)
                    └─► CONFIRMING  (bot presented order summary, waiting for confirmation)
                          ├─► ORDER_PLACED  (confirmed — happy path)
                          └─► BROWSING      (user wants to change — back to browsing)
```

Any state can transition to `IDLE` after a configurable inactivity timeout (default: 30 min).

## State Definitions

| State | Description |
|---|---|
| `IDLE` | No active conversation. Next message starts fresh. |
| `GREETING` | Bot sent welcome message. Waiting for user intent. |
| `BROWSING` | User is exploring the menu or asking questions. |
| `ORDERING` | User has selected an item. Bot is collecting details (size, qty). |
| `CONFIRMING` | Bot has presented the order summary. Waiting for yes/no. |
| `ORDER_PLACED` | Order registered. Conversation ends. |

## Session Storage (Redis)

Each `ConversationSession` is stored in Redis keyed by `sessionId`:

```json
{
  "sessionId": "1234567890@telegram",
  "state": "BROWSING",
  "providerName": "telegram",
  "recipientId": "1234567890",
  "currentOrderDraft": null,
  "history": [...],
  "createdAt": "2026-06-02T10:00:00Z",
  "updatedAt": "2026-06-02T10:05:00Z"
}
```

TTL: 30 minutes (refreshed on each message). On expiry, session is lost → starts fresh.

## Role of the AI

The AI drives the conversation in natural language. The state machine is used to:
1. Build the right system prompt for each state
2. Guard against out-of-order transitions
3. Know when to call which tools

The AI should be able to handle "quero uma calabresa grande" in a single message,
jumping directly to `CONFIRMING` without intermediate steps.

## System Prompt per State

Each state has a prompt fragment that the orchestrator prepends to the system prompt:

- `IDLE` / `GREETING`: introduce the pizzeria, ask how to help
- `BROWSING`: knows the menu, can answer questions, invite to order
- `ORDERING`: collecting pizza details, will confirm order
- `CONFIRMING`: presenting summary, asking for confirmation
- `ORDER_PLACED`: thank you message, end of conversation

## State Transitions (Responsibility)

State transitions are triggered by tool calls from the AI:
- `placeOrder(...)` called → transition to `CONFIRMING`
- `confirmOrder()` called → transition to `ORDER_PLACED`
- `cancelOrder()` called → transition to `BROWSING`

The `ConversationService` listens to tool call outcomes and updates the state in Redis.

## Phase 1 Implementation (MVP)

- [ ] `ConversationState` enum
- [ ] `ConversationSession` (Redis-serializable POJO)
- [ ] `ConversationRepository` (Redis `@Repository`)
- [ ] `ConversationService` interface
- [ ] `ConversationServiceImpl` (state transitions, TTL refresh)
- [ ] `ConversationRouter` (routes `InboundMessage` to `ConversationService`)
