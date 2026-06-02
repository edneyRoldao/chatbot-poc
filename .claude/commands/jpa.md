# JPA Conventions

## Query Strategy (preference order)

1. **Method query names** — for simple queries
2. **`@Query` (JPQL)** — when method name becomes too long
3. **Native SQL** — only when JPQL cannot express the query

Never use `JpaSpecificationExecutor` / `Specification<T>`.

In derived query names: use camelCase only, never underscore form.
`findBySessionId` (correct) — `findBySession_Id` (wrong).

## Entity Conventions

- Entities live in `{module}/domain/`
- Use `@Entity` + `@Table(name = "...")` with explicit table name
- Primary key: `UUID` with `gen_random_uuid()` default via `@GeneratedValue`
- Timestamps: `@CreationTimestamp` / `@UpdateTimestamp` (Hibernate)
- No `@Data` from Lombok — write explicit getters/setters or use records where possible

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
```

## Module Boundaries — Cross-Module Rules

Each module is an isolated bounded context at the entity layer.

**Forbidden across module boundaries:**
- `@ManyToMany` — never, anywhere
- `@ManyToOne` — only within the same module
- `@OneToOne` — only within the same module
- `@OneToMany` — only within the same module
- `mappedBy` (bidirectional) — never; all relationships unidirectional

**Cross-module references always use plain IDs:**

```java
// Wrong — crosses module boundary
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "session_id")
private ConversationSession session;

// Right — ID only
@Column(name = "session_id", nullable = false)
private String sessionId;
```

**Collections of foreign IDs use `@ElementCollection`:**

```java
@ElementCollection
@CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
@Column(name = "pizza_id")
private List<String> pizzaIds = new ArrayList<>();
```

## Flyway Migrations

Format: `V{yyyyMMddHHmmss}__{description}.sql`

Example: `V20260602120000__create_orders.sql`

- Never use `ddl-auto: create` or `ddl-auto: update` — always `validate`
- All schema changes go through Flyway scripts
- Include `NOT NULL`, defaults, and indexes in migrations — not in entity annotations

## DDL-Auto

Always `validate` in all environments. Never `create`, `create-drop`, or `update`.
