# AI Integration — LangChain4j + Groq

## Overview

LangChain4j is used as the AI orchestration layer. The model is configured against Groq's
OpenAI-compatible API (`https://api.groq.com/openai/v1`). Migrating to self-hosted Llama
later requires only changing `baseUrl` and `modelName` in configuration — no code changes.

## Model Configuration

```java
@Configuration
public class AiConfig {

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.base-url}")
    private String baseUrl;

    @Value("${app.ai.model}")
    private String model;

    @Bean
    public OpenAiChatModel chatModel() {
        return OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName(model)
            .maxTokens(1024)
            .temperature(0.7)
            .build();
    }
}
```

## Tool (Function Calling) Pattern

Tools are plain Java methods annotated with `@Tool`. LangChain4j generates the OpenAI
function schema automatically from method signatures and Javadoc.

```java
@Component
public class PizzeriaTools {

    private final MenuService menuService;
    private final OrderService orderService;

    @Tool("Returns the full pizzeria menu with available pizzas and prices")
    public String getMenu() {
        return menuService.getMenuAsText();
    }

    @Tool("Places an order for the customer")
    public String placeOrder(
        @P("Pizza name exactly as shown in the menu") String pizzaName,
        @P("Size: SMALL, MEDIUM, or LARGE") String size,
        @P("Quantity") int quantity,
        @P("Session ID of the conversation") String sessionId
    ) {
        return orderService.placeOrder(pizzaName, size, quantity, sessionId);
    }
}
```

## AI Service Interface

```java
public interface AiOrchestrator {
    String chat(String sessionId, String userMessage);
}
```

Implementation uses LangChain4j's `AiServices` or direct `ChatMemory` + `ChatLanguageModel`:

```java
@Service
class GroqAiOrchestrator implements AiOrchestrator {

    private final ChatLanguageModel chatModel;
    private final PizzeriaTools tools;
    private final ConversationRepository conversationRepository;

    @Override
    public String chat(String sessionId, String userMessage) {
        List<ChatMessage> history = loadHistory(sessionId);
        history.add(new UserMessage(userMessage));
        AiMessage response = sendToModel(history, tools);
        saveHistory(sessionId, history, response);
        return response.text();
    }
}
```

## System Prompt

The system prompt is loaded from `src/main/resources/prompts/system-prompt.txt`.
Never hardcode the system prompt inside Java classes — keep it as an external resource
so it can be iterated without recompiling.

## Memory / Conversation History

- Conversation history is stored in Redis as a `List<ChatMessage>` per `sessionId`
- Each turn: load history → add user message → call model → save assistant response
- History is capped at N last turns (configurable via `app.ai.max-history-turns`)
- History is cleared when `ConversationState` transitions to `ORDER_PLACED`

## Groq → Llama Migration Path

When migrating to self-hosted Llama (via Ollama or vLLM):
- Ollama is OpenAI API compatible → change `baseUrl` only
- vLLM is OpenAI API compatible → change `baseUrl` and `modelName`
- No Java code changes required

```yaml
# Groq (current)
app.ai.base-url: https://api.groq.com/openai/v1
app.ai.model: llama-3.3-70b-versatile

# Ollama (future local)
app.ai.base-url: http://localhost:11434/v1
app.ai.model: llama3.1:8b
```

## Error Handling

- On model timeout or API error → return a fallback message in PT-BR
- Log the error with session ID for debugging — never log the message content
- Never throw exceptions up to the messaging layer — always return a safe string response
