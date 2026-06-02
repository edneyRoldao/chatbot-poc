# Development Plan: Chatbot POC — Pizzeria MVP

Goal: validate the full AI + messaging orchestration stack using a pizzeria as the domain example,
before building the real pharmacy bot.

## Architecture Decisions

| Topic | Decision |
|-------|----------|
| Messaging providers | Strategy pattern — `MessagingProvider` interface; zero code change to add a new provider |
| First provider | Telegram (long polling in dev, webhook in prod) — no Meta Business account needed |
| Future provider | WhatsApp — same interface, new `@Component` only |
| AI provider | Groq (free, LLaMA models, OpenAI-compatible API) |
| AI framework | LangChain4j — tool calling, chat memory, provider-agnostic |
| AI migration path | Groq → Ollama/vLLM (local Llama): change `baseUrl` + `modelName` only |
| Project structure | Single Maven module, DDD-light modular packages |
| Session storage | Redis — `ConversationSession` keyed by `sessionId`, 30-min TTL |
| Order persistence | PostgreSQL via JPA + Flyway |
| Bot language | PT-BR |
| Payment (MVP) | Simulated — bot confirms order with a generated number, no gateway |
| Menu (MVP) | Hardcoded in `MenuService` — no database table |
| Domain example | Pizzaria do João — 5 pizzas, 3 sizes (Pequena / Média / Grande) |

---

## Phase 1 — Bootstrap
> Project skeleton, conventions, specs, and configuration. No business logic.

- [x] `CLAUDE.md` — universal conventions (composed method, OrFail, DRY, ternary, StringUtils)
- [x] `.claude/settings.json` — Maven permission allowlist + `PreToolUse` hook registration
- [x] `.claude/hooks/context-inject.sh` — auto-injects the right command file based on the Java file being edited
- [x] `.claude/commands/spring-arch.md` — Spring package structure, service flow, cross-module rules
- [x] `.claude/commands/jpa.md` — query strategy, entity conventions, Flyway, cross-module ID-only rule
- [x] `.claude/commands/messaging-strategy.md` — provider interface, session namespacing, registry pattern
- [x] `.claude/commands/ai-integration.md` — LangChain4j config, tool pattern, memory, Groq → Llama migration
- [x] `.claude/commands/telegram.md` — long polling setup, bot registration, session ID format, webhook mode
- [x] `specs/messaging-strategy.md` — full inbound/outbound flow, provider registration contract
- [x] `specs/conversation-flow.md` — state machine definition, Redis session structure, AI role
- [x] `specs/pizzeria-menu.md` — menu items, sizes, prices, `MenuItem` domain object
- [x] `specs/ai-orchestration.md` — system prompt, tools, history strategy, error handling
- [x] `pom.xml` — Spring Boot 4.0.4, Java 21, LangChain4j 1.0.0-rc1, TelegramBots 6.9.7.1, Flyway, Redis, PostgreSQL
- [x] `ChatbotPocApplication.java` — entry point
- [x] `application.yml` — shared behavioral config (no credentials)
- [x] `application-dev.yml` — dev profile (local infra at 192.168.68.53, secrets from env vars)
- [x] `application-prd.yml` — production profile (all values from env vars, no fallbacks)
- [x] `.env.example` — documents required environment variables
- [x] `.gitignore` — `target/`, `.env`, `.idea/`, `settings.local.json`
- [x] `db/migration/V20260602120000__create_orders.sql` — `orders` table with indexes
- [x] `db/migration/V20260602120001__create_order_items.sql` — `order_items` table
- [x] `prompts/system-prompt.txt` — PT-BR system prompt for the pizzeria bot

---

## Phase 2 — Messaging Layer
> `MessagingProvider` interface + Telegram implementation (long polling). No AI yet — just echo.

- [x] `messaging/domain/InboundMessage.java` — record: `sessionId`, `senderId`, `text`, `providerName`
- [x] `messaging/domain/OutboundMessage.java` — record: `recipientId`, `text`
- [x] `messaging/service/MessagingProvider.java` — interface: `send(OutboundMessage)`, `providerName()`
- [x] `messaging/component/MessagingProviderRegistry.java` — `Map<String, MessagingProvider>` built from `List<MessagingProvider>` injection
- [x] `messaging/service/impl/TelegramMessagingProvider.java` — `@Component`, implements `MessagingProvider`, `providerName() = "telegram"`
- [x] `messaging/config/TelegramBotConfig.java` — `TelegramLongPollingBot` bean extending class with `onUpdateReceived`
- [x] `messaging/component/TelegramBotRegistrar.java` — `ApplicationRunner` that registers the bot with `TelegramBotsApi`
- [x] `messaging/component/ConversationRouter.java` — receives `InboundMessage`, delegates to `ConversationService`, sends reply via registry
- [ ] Smoke test: bot receives "oi" → echoes back via Telegram

---

## Phase 3 — Conversation + State Machine
> Redis-backed session, state transitions, and routing. Still no AI — deterministic responses.

- [x] `conversation/domain/ConversationState.java` — enum: `IDLE`, `GREETING`, `BROWSING`, `ORDERING`, `CONFIRMING`, `ORDER_PLACED`
- [x] `conversation/domain/ConversationSession.java` — POJO: `sessionId`, `state`, `providerName`, `recipientId`, `currentOrderDraft`, `history`, `createdAt`, `updatedAt`
- [x] `shared/config/RedisConfig.java` — `RedisTemplate<String, ConversationSession>` with Jackson serializer
- [x] `conversation/repository/ConversationRepository.java` — Redis `@Repository`: `findById`, `save`, `deleteById`
- [x] `conversation/service/ConversationService.java` — interface: `handle(InboundMessage)`
- [x] `conversation/service/impl/ConversationServiceImpl.java` — loads/creates session, advances state, refreshes TTL
- [x] Smoke test: first message creates session in `GREETING` state; verify via Redis CLI

---

## Phase 4 — AI + Menu
> LangChain4j + Groq integrated. Bot answers in natural language using tools.

- [x] `menu/domain/PizzaSize.java` — enum: `SMALL`, `MEDIUM`, `LARGE` with display name + slice count
- [x] `menu/domain/MenuItem.java` — record: `name`, `description`, `Map<PizzaSize, BigDecimal> prices`
- [x] `menu/service/MenuService.java` — interface: `getMenuAsText()`, `findItem(String name, PizzaSize size)`
- [x] `menu/service/impl/MenuServiceImpl.java` — hardcoded list of 5 pizzas; no database
- [x] `ai/config/AiConfig.java` — `OpenAiChatModel` bean pointing to Groq; reads from `app.ai.*` properties
- [x] `ai/component/PizzeriaTools.java` — `@Component` with `@Tool` methods: `getMenu()`, `placeOrder()`, `cancelOrder()`
- [x] `ai/service/AiOrchestrator.java` — interface: `chat(ConversationSession session, String userMessage) → String`
- [x] `ai/service/impl/GroqAiOrchestrator.java` — loads history from Redis, calls Groq with tools, saves response
- [x] Wire `AiOrchestrator` into `ConversationServiceImpl` — replace deterministic echo with AI response
- [x] Smoke test: "quero ver o cadapio" → bot responds with full menu in PT-BR ✓

---

## Phase 5 — Order + End-to-End Integration
> Order persisted to PostgreSQL. Full flow: Telegram → AI → tool call → DB → confirmation.

- [ ] `order/domain/OrderStatus.java` — enum: `PENDING`, `CONFIRMED`, `CANCELLED`
- [ ] `order/domain/Order.java` — JPA entity mapped to `orders` table
- [ ] `order/domain/OrderItem.java` — JPA entity mapped to `order_items` table
- [ ] `order/repository/OrderRepository.java` — Spring Data JPA
- [ ] `order/dto/OrderCreateRequest.java` — internal DTO used by `PizzeriaTools`
- [ ] `order/dto/OrderResponse.java` — returned by `OrderService`
- [ ] `order/mapper/OrderMapper.java` — entity ↔ DTO
- [ ] `order/service/OrderService.java` — interface: `placeOrder(...)`, `cancelOrder(String sessionId)`
- [ ] `order/service/impl/OrderServiceImpl.java` — validates item against `MenuService`, persists order, returns confirmation string
- [ ] Wire `OrderService` into `PizzeriaTools.placeOrder()` — replace simulation with real persistence
- [ ] Update `ConversationServiceImpl` to transition to `ORDER_PLACED` after successful `placeOrder` tool call
- [ ] End-to-end smoke test:
  - Send "quero uma calabresa grande" via Telegram
  - AI collects details and presents summary
  - Confirm with "sim"
  - Verify `orders` row created in PostgreSQL
  - Verify session state is `ORDER_PLACED` in Redis
  - Verify Telegram receives final confirmation message

---

## Verification (each phase)

1. `mvn compile` — zero errors before moving to next phase
2. `mvn test` — existing tests pass
3. Phase 2+: bot registered and responds on Telegram
4. Phase 3+: `redis-cli keys "*"` shows session key after first message
5. Phase 4+: bot calls `getMenu()` correctly and responds in PT-BR
6. Phase 5: `orders` row present in PostgreSQL after full conversation flow
