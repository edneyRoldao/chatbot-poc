# Spring Architecture Conventions

## Package Structure — DDD Light (Modular)

Every module has explicit sub-packages. No class lives at module root.

```
{module}/
├── domain/        ← JPA entities, domain enums, value objects
├── dto/           ← Request/response records (one file per DTO)
├── mapper/        ← Mapper classes (MapStruct or manual @Component)
├── repository/    ← Spring Data repository interfaces
├── service/       ← Service interfaces only
├── service/impl/  ← Service implementations (*Impl, package-private)
├── component/     ← @Component helpers (builders, validators, etc.)
├── controller/    ← @RestController (only if module exposes REST)
└── config/        ← @Configuration classes scoped to this module
```

**Project modules:** `messaging`, `conversation`, `ai`, `menu`, `order`, `shared`, `app`

## Service Method Flow Style

Public business methods must read as a sequential flow:

```java
@Transactional
public OrderResponse create(OrderCreateRequest request) {
    ConversationSession session = resolveSessionOrFail(request.sessionId());
    validateOrderRequest(request);
    Order order = buildOrder(request, session);
    Order saved = orderRepository.save(order);
    publishOrderPlaced(saved);
    return orderMapper.toResponse(saved);
}

private ConversationSession resolveSessionOrFail(String sessionId) {
    return conversationRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));
}

private void validateOrderRequest(OrderCreateRequest request) {
    checkItemsNotEmptyOrFail(request.items());
}
```

## Guard Clauses

Extract all exception-throwing `if` blocks into private methods with `OrFail` suffix:

```java
private void checkItemsNotEmptyOrFail(List<OrderItemInput> items) {
    if (CollectionUtils.isEmpty(items)) {
        throw new ValidationException("Order must have at least one item");
    }
}
```

## Repository Lookups

Extract repeated `findBy…().orElseThrow(…)` into `OrFail` helpers:

```java
private Order findOrderOrFail(UUID orderId) {
    return orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
}
```

## DTOs

- Every DTO lives in its own file under `dto/` package — no inner classes in controllers
- Use Java records for immutable DTOs
- Example: `order/dto/OrderCreateRequest.java`, `order/dto/OrderResponse.java`

## Controllers

- Handle only: parameter binding, validation delegation, HTTP status, service call
- Business logic belongs in the service layer
- Every endpoint declares `@ResponseStatus` explicitly

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public OrderResponse create(@Valid @RequestBody OrderCreateRequest request) {
    return orderService.create(request);
}
```

## Mappers

- One mapper per module under `mapper/`
- Use MapStruct for straightforward mappings; manual `@Component` when Spring beans are needed
- Mappers never import entities or types from other modules

## Cross-Module Dependencies

Service **interfaces** never expose types from other modules in method signatures.
Implementations resolve cross-module data internally:

```java
// Wrong — interface leaks another module's type
public interface OrderService {
    OrderResponse create(OrderCreateRequest request, ConversationSession session);
}

// Right — impl resolves internally
class OrderServiceImpl implements OrderService {
    private final ConversationRepository conversationRepository; // internal detail
}
```

## Boolean Negation

Always extract negative boolean into a named method:

```java
// Wrong
.filter(order -> !isCancelled(order))

// Right
.filter(this::isNotCancelled)
private boolean isNotCancelled(Order order) { return order.status() != OrderStatus.CANCELLED; }
```
