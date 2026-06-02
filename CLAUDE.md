# Chatbot POC — Conventions

## Code Language
All code in English: variable names, method names, class names, constants, comments, and log messages.
Spec files in `specs/` must also be written in English.

## Core Principles
- Change only what the task requires — no broad refactors, no extra features
- Descriptive, intention-revealing names — explicit over abbreviated
- Import only what is used, never wildcard imports
- No explanatory comments; express intent through small, focused methods with clear names
- No "Co-Authored-By" in commit messages

## Composed Method Pattern
Top-level public methods read like a sequential script. Every `if` block, loop, or I/O step
is extracted into a named private method.

```java
@Transactional
public OrderResponse create(OrderCreateRequest request) {
    ConversationSession session = resolveSessionOrFail(request.sessionId());
    Order order = buildOrder(request, session);
    Order saved = orderRepository.save(order);
    publishOrderPlaced(saved);
    return orderMapper.toResponse(saved);
}
```

## Boolean Negation
Never negate inline. Extract a named method:

```java
// Wrong
.filter(item -> !isUnavailable(item))

// Right
.filter(this::isAvailable)
private boolean isAvailable(MenuItem item) { return !unavailableIds.contains(item.id()); }
```

## Guard Clauses — `OrFail` Suffix
Exception-throwing guards become private methods with `OrFail` suffix:

```java
// Wrong
if (!menuService.exists(pizzaId)) throw new NotFoundException("Pizza not found: " + pizzaId);

// Right
checkPizzaExistsOrFail(pizzaId);

private void checkPizzaExistsOrFail(String pizzaId) {
    if (!menuService.exists(pizzaId)) throw new NotFoundException("Pizza not found: " + pizzaId);
}
```

## Repository Lookups
Any `repository.findBy…().orElseThrow(…)` used more than once must be extracted
into a private helper with `OrFail` suffix.

## Ternary Operators
Never pass ternary expressions directly as method arguments. Assign to named variable first:

```java
// Wrong
service.confirm(request.items() != null ? request.items() : List.of(), sessionId);

// Right
List<OrderItemInput> items = request.items() != null ? request.items() : List.of();
service.confirm(items, sessionId);
```

## Method Ordering
Public methods always before private methods. Never intersperse them.

## String Utilities
Always use `org.apache.commons.lang3.StringUtils`:
- `StringUtils.isBlank(s)` / `StringUtils.isNotBlank(s)` — null + blank check
- `StringUtils.trimToNull(s)` / `StringUtils.trimToEmpty(s)`

Never write `s != null && !s.isBlank()` or private helpers for this.

## DRY
Any logic repeated more than once must be extracted into a named method, constant, or helper.

## Properties — Three-Profile Strategy
| File | Purpose |
|---|---|
| `application.yml` | Shared behavioral config only. No credentials, no secrets. |
| `application-dev.yml` | Local dev. Secrets come from `.env` (gitignored). |
| `application-prd.yml` | Production. Every credential via env var, no fallback. |

Never edit `application.yml` for testing — use env vars or the dev profile.

## Deprecated APIs
Never use classes or methods marked `@Deprecated`. Always use the documented replacement.
Compiler warnings for deprecation are treated as errors — do not suppress them.

### Spring Boot 4.x / Spring Framework 7.x replacements
| Deprecated | Replacement |
|---|---|
| `org.springframework.lang.NonNull` | `org.jspecify.annotations.NonNull` |
| `org.springframework.lang.Nullable` | `org.jspecify.annotations.Nullable` |
| `HttpStatus.UNPROCESSABLE_ENTITY` | `HttpStatus.UNPROCESSABLE_CONTENT` |
| `Jackson2JsonRedisSerializer` (`com.fasterxml`) | `JacksonJsonRedisSerializer` (`tools.jackson`) |

### Jackson 2.x vs 3.x
Spring Boot 4.x uses Jackson 3.x (`tools.jackson.*` packages). Always import from `tools.jackson`,
never from `com.fasterxml.jackson` for Spring-managed beans and serializers.
The auto-configured `ObjectMapper` bean is `tools.jackson.databind.ObjectMapper`.

## Detailed Conventions by Area
- Spring package structure and service patterns → `.claude/commands/spring-arch.md`
- JPA query strategy and cross-module rules → `.claude/commands/jpa.md`
- Messaging Strategy pattern and provider contract → `.claude/commands/messaging-strategy.md`
- LangChain4j + Groq integration patterns → `.claude/commands/ai-integration.md`
- Telegram Bot setup and polling → `.claude/commands/telegram.md`
