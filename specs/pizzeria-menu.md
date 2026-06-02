# Spec: Pizzeria Menu

## Domain

The domain example is "Pizzaria do João" — a fictional Brazilian pizzeria used to validate
the chat automation concept. The menu is hardcoded for MVP (no database table needed).

## Menu Items

### Pizzas

| Name | Description | Small (4 slices) | Medium (6 slices) | Large (8 slices) |
|---|---|---|---|---|
| Margherita | Molho de tomate, mussarela, manjericão fresco | R$ 29,00 | R$ 39,00 | R$ 49,00 |
| Calabresa | Linguiça calabresa, mussarela, cebola | R$ 32,00 | R$ 42,00 | R$ 52,00 |
| Frango com Catupiry | Frango desfiado, catupiry, mussarela | R$ 35,00 | R$ 45,00 | R$ 55,00 |
| Pepperoni | Pepperoni importado, mussarela, azeitona | R$ 38,00 | R$ 48,00 | R$ 58,00 |
| Portuguesa | Presunto, ovos, cebola, mussarela, azeitona | R$ 36,00 | R$ 46,00 | R$ 56,00 |

### Sizes

| Code | Display | Slices |
|---|---|---|
| `SMALL` | Pequena | 4 fatias |
| `MEDIUM` | Média | 6 fatias |
| `LARGE` | Grande | 8 fatias |

## Menu Service Contract

`MenuService` exposes two methods:
1. `getMenuAsText()` → formatted string the AI can include in responses
2. `findItem(String pizzaName, String size)` → returns `Optional<MenuItem>` for order validation

## Menu Text Format (for AI context)

The `getMenuAsText()` return value is injected as tool result when the AI calls `getMenu()`:

```
🍕 Cardápio da Pizzaria do João

PIZZAS DISPONÍVEIS:
• Margherita — Molho de tomate, mussarela, manjericão fresco
• Calabresa — Linguiça calabresa, mussarela, cebola
• Frango com Catupiry — Frango desfiado, catupiry, mussarela
• Pepperoni — Pepperoni importado, mussarela, azeitona
• Portuguesa — Presunto, ovos, cebola, mussarela, azeitona

TAMANHOS E PREÇOS:
         Pequena (4 fatias)  Média (6 fatias)  Grande (8 fatias)
Margherita        R$ 29,00         R$ 39,00          R$ 49,00
Calabresa         R$ 32,00         R$ 42,00          R$ 52,00
Frango c/ Cat.    R$ 35,00         R$ 45,00          R$ 55,00
Pepperoni         R$ 38,00         R$ 48,00          R$ 58,00
Portuguesa        R$ 36,00         R$ 46,00          R$ 56,00
```

## MenuItem Domain Object

```java
public record MenuItem(
    String name,
    String description,
    Map<PizzaSize, BigDecimal> prices
) {
    public BigDecimal priceFor(PizzaSize size) {
        return prices.get(size);
    }
}
```

## PizzaSize Enum

```java
public enum PizzaSize {
    SMALL("Pequena", 4),
    MEDIUM("Média", 6),
    LARGE("Grande", 8);

    private final String displayName;
    private final int slices;
}
```

## Ordering Rules

1. Customer must select: pizza name + size + quantity
2. Bot confirms with total price before placing
3. For MVP: no toppings customization, no half-and-half
4. Out-of-stock simulation: all items always available in MVP
