# Spec: AI Orchestration

## Goal

Use LangChain4j + Groq to drive natural language conversation for the pizzeria bot.
The AI interprets user intent, calls tools to interact with domain services,
and generates responses in PT-BR.

## Model

- Provider: Groq (`https://api.groq.com/openai/v1`)
- Model: `llama-3.3-70b-versatile` (default, configurable via `app.ai.model`)
- Max tokens: 1024
- Temperature: 0.7

## System Prompt

Location: `src/main/resources/prompts/system-prompt.txt`

Content:

```
Você é o assistente virtual da Pizzaria do João, uma pizzaria brasileira.
Seu nome é João Bot.

Seu objetivo é ajudar os clientes a fazer pedidos de pizza de forma amigável e eficiente.

REGRAS:
- Sempre responda em português brasileiro
- Seja simpático e informal, como um atendente de pizzaria
- Quando o cliente quiser ver o cardápio, use a ferramenta getMenu()
- Quando o cliente quiser fazer um pedido, colete: nome da pizza, tamanho e quantidade
- Antes de confirmar o pedido, sempre apresente o resumo com o valor total
- Só registre o pedido com placeOrder() após a confirmação explícita do cliente
- Se o cliente cancelar, volte ao estado de navegação e ofereça ajuda
- Não invente pizzas ou preços que não estejam no cardápio
- Tempo estimado de entrega: sempre 35 a 45 minutos

FLUXO ESPERADO:
1. Cumprimentar o cliente
2. Perguntar o que deseja (ver cardápio ou pedir diretamente)
3. Coletar os dados do pedido (pizza, tamanho, quantidade)
4. Apresentar o resumo com o valor total
5. Confirmar com o cliente
6. Registrar o pedido com placeOrder()
7. Agradecer e informar o tempo estimado
```

## Tools Available to the AI

| Tool | Signature | Description |
|---|---|---|
| `getMenu` | `() → String` | Returns the full menu text |
| `placeOrder` | `(pizzaName, size, quantity, sessionId) → String` | Creates the order |
| `cancelOrder` | `(sessionId) → String` | Cancels the order in progress |

## Conversation Memory

History stored in Redis as a list of messages per `sessionId`:
- `SystemMessage` (system prompt, always first)
- `UserMessage` (from customer)
- `AiMessage` (bot responses)
- `ToolExecutionResultMessage` (tool responses)

Max history: 20 messages (configurable via `app.ai.max-history-turns`).
Older messages are trimmed from the front, keeping `SystemMessage` always.

## Request Flow

```
InboundMessage(sessionId, text)
  └─► ConversationService.handle(inbound)
        └─► AiOrchestrator.chat(sessionId, text)
              ├─► load history from Redis
              ├─► append UserMessage
              ├─► call ChatLanguageModel (Groq)
              │     ├─► model may call tools (getMenu, placeOrder, etc.)
              │     └─► model returns AiMessage with text
              ├─► append AiMessage to history
              ├─► save history to Redis
              └─► return response text
        └─► MessagingProvider.send(OutboundMessage(recipientId, responseText))
```

## Tool Call Handling

LangChain4j handles the tool call loop automatically:
1. Model returns a tool call request
2. LangChain4j invokes the annotated Java method
3. Result is appended as `ToolExecutionResultMessage`
4. Model is called again with the result
5. Loop continues until model returns a plain text response

The Java tool methods (`PizzeriaTools`) are injected into the `AiServices` builder.

## Error Handling

| Error | Behavior |
|---|---|
| Groq API timeout | Return: "Ops! Tivemos um problema técnico. Pode tentar novamente?" |
| Groq API rate limit | Return: same fallback + log warning with sessionId |
| Tool throws exception | Catch internally, return error string to model, let model handle gracefully |
| Invalid pizza in order | `placeOrder()` returns error string; model tells the customer |

## Phase 1 Implementation (MVP)

- [ ] `AiOrchestrator` interface
- [ ] `GroqAiOrchestrator` implementation
- [ ] `AiConfig` with `OpenAiChatModel` bean
- [ ] `PizzeriaTools` with `getMenu()`, `placeOrder()`, `cancelOrder()`
- [ ] `system-prompt.txt` resource file
- [ ] History loading/saving in Redis
