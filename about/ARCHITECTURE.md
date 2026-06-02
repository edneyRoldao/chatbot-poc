# Pharmacy Bot — Arquitetura Detalhada

## Diagrama de Componentes

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENTE                                  │
│                    (WhatsApp Mobile)                            │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTPS
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                   META CLOUD API                                │
│              (WhatsApp Business Platform)                       │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Webhook POST
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│               WEBHOOK RECEIVER (NestJS)                         │
│  - Valida assinatura HMAC do Meta                               │
│  - Faz parse do payload (texto, imagem, PDF, áudio)             │
│  - Recupera/cria sessão no Redis                                │
│  - Delega ao Orquestrador via HTTP interno                      │
└──────────┬───────────────────────────────────────┬──────────────┘
           │                                       │
           ▼                                       ▼
    ┌─────────────┐                        ┌──────────────┐
    │    Redis    │                        │  Orquestrador │
    │  (Sessão)   │◄───────────────────────│  Spring Boot  │
    └─────────────┘                        │ LangChain4j   │
                                           └──────┬────────┘
                                                  │
                        ┌─────────────────────────┼──────────────────────────┐
                        │                         │                          │
                        ▼                         ▼                          ▼
              ┌──────────────────┐    ┌────────────────────┐    ┌────────────────────┐
              │  Vision Service  │    │   API Medicamentos  │    │  Checkout Service  │
              │  (vLLM + Qwen2)  │    │   (API da farmácia) │    │  (Mercado Pago /   │
              │  OCR de receitas │    │   estoque + preço   │    │   PagSeguro)       │
              └──────────────────┘    └────────────────────┘    └────────────────────┘
                        │
                        ▼
              ┌──────────────────┐
              │   LLM Diálogo    │
              │  (vLLM + Llama)  │
              │  NLU + geração   │
              └──────────────────┘

                        │ Pedido fechado
                        ▼
              ┌──────────────────┐         ┌──────────────────┐
              │    RabbitMQ      │────────►│  Handoff Worker  │
              │  (fila pedidos)  │         │  envia para WA   │
              └──────────────────┘         │  do atendente    │
                                           └──────────────────┘
                        │
                        ▼
              ┌──────────────────┐
              │   PostgreSQL     │
              │  (pedidos +      │
              │   auditoria)     │
              └──────────────────┘
```

---

## State Machine da Conversa

```
                    ┌─────────┐
                    │  IDLE   │ ← nova conversa / timeout
                    └────┬────┘
                         │ mensagem recebida
                         ▼
                  ┌─────────────┐
                  │  GREETING   │ → apresenta o bot, pede receita ou pergunta
                  └──────┬──────┘
                         │
              ┌──────────┴──────────┐
              │                     │
              ▼                     ▼
    ┌──────────────────┐   ┌──────────────────┐
    │ AWAITING_RECIPE  │   │  FREE_INQUIRY    │
    │ (aguarda imagem/ │   │ (dúvidas gerais, │
    │  PDF da receita) │   │  disponibilidade)│
    └────────┬─────────┘   └──────────────────┘
             │ imagem/PDF recebido
             ▼
    ┌──────────────────────┐
    │   PROCESSING_OCR     │ → chama Vision Service
    └────────┬─────────────┘
             │
     ┌───────┴────────────────────────────┐
     │                                    │
     ▼                                    ▼
┌──────────────┐                  ┌───────────────┐
│  CONTROLLED  │                  │  OCR_FAILED   │
│  MEDICATION  │                  │ (baixa conf.) │
│  → HANDOFF   │                  │  → HANDOFF    │
└──────────────┘                  └───────────────┘
     │ (caminho feliz)
     ▼
┌──────────────────────┐
│  CONFIRMING_RECIPE   │ → exibe resumo extraído, aguarda confirmação
└────────┬─────────────┘
         │ cliente confirma
         ▼
┌──────────────────────┐
│  CHECKING_STOCK      │ → consulta API de medicamentos
└────────┬─────────────┘
         │
     ┌───┴───────────────────────────┐
     │                               │
     ▼                               ▼
┌──────────────┐             ┌───────────────┐
│  OUT_OF_STOCK│             │ PRESENTING    │
│  (sem estoque│             │ OFFER         │
│  → handoff  │             │(preço, genér.)│
│  ou avisar) │             └───────┬───────┘
└──────────────┘                    │ cliente aceita
                                    ▼
                           ┌──────────────────┐
                           │ COLLECTING_      │
                           │ DELIVERY_INFO    │ → endereço ou retirada
                           └───────┬──────────┘
                                   │
                                   ▼
                           ┌──────────────────┐
                           │   CHECKOUT       │ → gera PIX / link
                           └───────┬──────────┘
                                   │ pagamento confirmado
                                   ▼
                           ┌──────────────────┐
                           │  ORDER_PLACED    │ → evento RabbitMQ
                           └───────┬──────────┘
                                   │
                                   ▼
                           ┌──────────────────┐
                           │    SILENT        │ ← bot para aqui
                           │ (humano assume   │    humano cuida do resto
                           │  a entrega)      │
                           └──────────────────┘
```

---

## Modelo de Dados (PostgreSQL)

### Tabela: `orders`
```sql
CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    whatsapp_number VARCHAR(20) NOT NULL,        -- número do cliente (hashed ou mascarado)
    status          VARCHAR(30) NOT NULL,         -- placed, processing, delivered, cancelled
    delivery_type   VARCHAR(10) NOT NULL,         -- pickup | delivery
    delivery_address JSONB,                       -- apenas se delivery
    payment_method  VARCHAR(10) NOT NULL,         -- pix | link
    payment_ref     VARCHAR(100),                 -- referência do gateway
    payment_status  VARCHAR(20) NOT NULL,         -- pending | paid | failed
    operator_notified_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);
```

### Tabela: `order_items`
```sql
CREATE TABLE order_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID REFERENCES orders(id),
    medication_name  VARCHAR(200) NOT NULL,
    dosage           VARCHAR(100),
    quantity         INT NOT NULL,
    unit_price       NUMERIC(10,2),
    total_price      NUMERIC(10,2)
);
```

### Tabela: `handoffs`
```sql
CREATE TABLE handoffs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    whatsapp_number VARCHAR(20) NOT NULL,
    reason          VARCHAR(50) NOT NULL,   -- controlled | ocr_failed | error | out_of_stock
    operator_number VARCHAR(20) NOT NULL,
    context_snapshot JSONB,                 -- estado da conversa no momento do handoff
    created_at      TIMESTAMPTZ DEFAULT NOW()
);
```

### Tabela: `audit_log`
```sql
CREATE TABLE audit_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  VARCHAR(100),
    event_type  VARCHAR(50),               -- ocr_processed | handoff_triggered | order_placed
    metadata    JSONB,                     -- sem dados sensíveis de receita
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
```

---

## Contrato da API de Medicamentos (esperado)

O orquestrador vai chamar a API da farmácia com este contrato sugerido:

```
GET /medications/search?name={name}&dosage={dosage}

Response:
{
  "found": true,
  "medications": [
    {
      "id": "med-123",
      "name": "Amoxicilina 500mg",
      "brand": "Amoxil",
      "generic": true,
      "price": 18.90,
      "in_stock": true,
      "stock_quantity": 45,
      "requires_prescription": true,
      "controlled_class": null
    }
  ]
}
```

---

## Variáveis de Ambiente (referência)

```env
# Meta WhatsApp
WHATSAPP_TOKEN=
WHATSAPP_PHONE_NUMBER_ID=
WHATSAPP_VERIFY_TOKEN=
WHATSAPP_OPERATOR_NUMBER=+5511...    # número do atendente

# LLM (vLLM)
VISION_LLM_URL=http://vllm-vision:8001
DIALOG_LLM_URL=http://vllm-dialog:8002
OCR_CONFIDENCE_THRESHOLD=0.80

# API Farmácia
PHARMACY_API_URL=
PHARMACY_API_KEY=

# Pagamento
PAYMENT_GATEWAY=mercadopago          # ou pagseguro
PAYMENT_API_KEY=

# Infra
REDIS_URL=redis://redis:6379
RABBITMQ_URL=amqp://rabbitmq:5672
DATABASE_URL=postgresql://...

# Segurança
HANDOFF_TIMEOUT_MINUTES=10           # tempo até avisar cliente que humano não respondeu
```
