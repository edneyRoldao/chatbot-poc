# Pharmacy Bot — Contexto do Projeto

## Visão Geral

Automação de atendimento para rede de farmácias via WhatsApp.
O bot recebe receitas médicas (PDF ou imagem), lê os medicamentos, consulta disponibilidade via API própria, conduz o cliente até o checkout (PIX ou link de pagamento) e fecha o pedido para um humano finalizar a separação e envio.

---

## Decisões de Negócio

| Decisão | Definição |
|---|---|
| Canal | WhatsApp exclusivamente (Meta Cloud API) |
| Checkout | PIX + link de pagamento gerado na conversa |
| Entrega | Bot fecha o pedido; humano cuida da separação e logística |
| Público-alvo | Rede de farmácias específica (não SaaS genérico) |
| Receitas aceitas | Digitadas e manuscritas (PDF e imagem) |
| Controlados | C1, C2, antimicrobianos → handoff humano obrigatório |
| Persistência de dados | Apenas dados do pedido; imagem/texto da receita NÃO é armazenado (LGPD) |

---

## Fluxo de Atendimento

```
1. Cliente envia mensagem/imagem/PDF no WhatsApp
2. Webhook recebe → LLM identifica intenção
3. É receita?
   ├── Sim → Vision Service extrai texto da imagem/PDF
   │     ├── Contém medicamento controlado? → HANDOFF IMEDIATO
   │     ├── Confiança OCR < threshold?     → HANDOFF com contexto
   │     └── Ok → LLM estrutura entidades (med, dosagem, qtd, posologia)
   └── Não → fluxo de atendimento geral (dúvidas, disponibilidade, etc.)
4. Consulta API de medicamentos (estoque, preço, genéricos)
5. Bot apresenta resumo ao cliente e solicita confirmação
6. Cliente confirma → Bot coleta endereço (entrega) ou confirma retirada
7. Checkout Service gera PIX ou link de pagamento
8. Pedido fechado → evento publicado no RabbitMQ
9. Worker notifica atendente humano no WhatsApp da farmácia
   (resumo do pedido + forma de envio + comprovante de pagamento)
10. Bot envia confirmação ao cliente e entra em modo silencioso
```

### Regras de Handoff

- Medicamento controlado detectado → handoff antes de qualquer processamento
- OCR com confiança baixa → handoff com imagem original + contexto extraído
- Qualquer erro não tratado → handoff com log do estado atual
- Quando humano assume → bot para completamente (não reassume)
- Handoff = redirecionamento para outro número WhatsApp do atendente
- Se atendente não responder em X minutos → cliente recebe mensagem de status (não pode ficar em silêncio)

---

## Arquitetura de Sistema

```
WhatsApp (cliente)
       │
  [Meta Cloud API]
       │
  [Webhook Receiver — NestJS] ← recebe eventos, gerencia sessão
       │
  [Redis] ← estado da conversa (session store)
       │
  [Orquestrador de Fluxo — Spring Boot + LangChain4j]
       ├── [Vision Service] ← Qwen2-VL ou Llama 3.2 Vision via vLLM
       ├── [LLM de Diálogo] ← Llama 3.1 8B ou Mistral 7B via vLLM
       ├── [API de Medicamentos] ← API própria da farmácia
       ├── [Checkout Service] ← Mercado Pago ou PagSeguro (PIX + link)
       └── [Handoff Service] ← envia para WhatsApp do atendente
       │
  [RabbitMQ] ← eventos assíncronos (pedido fechado → fila de operadores)
  [PostgreSQL] ← pedidos e logs de auditoria (sem dados de receita)
```

---

## Stack Tecnológica

| Camada | Tecnologia | Observação |
|---|---|---|
| Canal | Meta WhatsApp Cloud API | Sandbox gratuito para dev (1000 conv/mês) |
| Webhook / sessão | NestJS + TypeScript | Leve, recebe eventos, delega ao backend |
| Orquestrador IA | Spring Boot + LangChain4j | Java nativo, integração direta com Spring |
| LLM runtime | vLLM (on-premise) | Melhor throughput em produção vs Ollama |
| Vision/OCR | Qwen2-VL 7B ou Llama 3.2 Vision 11B | Multimodal, suporte a manuscrito em PT-BR |
| LLM diálogo | Llama 3.1 8B ou Mistral 7B | Rápido, suficiente para NLU e orquestração |
| Fila de eventos | RabbitMQ | Já dominado pelo time |
| Cache / sessão | Redis | Estado da conversa por session ID |
| Pagamento | Mercado Pago ou PagSeguro | PIX + link de pagamento |
| Banco de dados | PostgreSQL | Apenas pedidos, sem dados sensíveis de receita |
| Infra | Docker + Kubernetes | Já dominado pelo time |
| Tunnel dev local | Cloudflare Tunnel | Gratuito, sem expiração (para expor webhook local) |

---

## LLM On-Premise — Detalhes

### Dois modelos com papéis distintos

| Papel | Modelo | Hardware mínimo |
|---|---|---|
| OCR + extração de receita | Qwen2-VL 7B ou Llama 3.2 Vision 11B | GPU 24GB VRAM |
| Orquestração / diálogo | Llama 3.1 8B ou Mistral 7B | Pode compartilhar GPU acima |

### Hardware recomendado para produção
- Mínimo: 1x NVIDIA A10G (24GB) ou 2x RTX 4090
- vLLM serve ambos os modelos com gerenciamento de memória eficiente

### Servindo os modelos
```bash
# Exemplo vLLM para modelo de visão
vllm serve Qwen/Qwen2-VL-7B-Instruct --port 8001

# Exemplo vLLM para modelo de diálogo
vllm serve meta-llama/Llama-3.1-8B-Instruct --port 8002
```

---

## Entidades Extraídas da Receita

O LLM deve retornar sempre neste formato (JSON):

```json
{
  "confidence": 0.92,
  "controlled": false,
  "medications": [
    {
      "name": "Amoxicilina",
      "dosage": "500mg",
      "quantity": 21,
      "posology": "1 comprimido a cada 8 horas por 7 dias",
      "controlled_class": null
    }
  ],
  "raw_text": null
}
```

- `confidence < 0.8` → disparar handoff
- `controlled: true` → disparar handoff antes de qualquer consulta à API
- `raw_text` deve ser nulo após processamento (não persistir)

---

## Regras de Compliance (LGPD)

- Imagem/PDF da receita: processar em memória, nunca persistir em disco ou banco
- Texto extraído da receita: descartar após extração das entidades
- Nunca logar o payload raw do WhatsApp (pode conter mídia codificada)
- Armazenar apenas: ID do pedido, medicamentos solicitados, endereço de entrega, forma de pagamento, status
- Dados de saúde sensíveis (diagnóstico, CID, nome do paciente) não devem entrar no banco

---

## Configuração WhatsApp Sandbox (Dev)

```
1. developers.facebook.com → Criar app → tipo "Business"
2. Adicionar produto: WhatsApp
3. WhatsApp > API Setup → copiar Access Token + Phone Number ID
4. Configurar Webhook URL + Verify Token
5. Cadastrar até 5 números reais como testers
6. Usar Cloudflare Tunnel para expor webhook local:
   cloudflared tunnel --url http://localhost:8080
```

---

## Próximos Passos Sugeridos (Ordem)

1. [ ] Setup do projeto NestJS (webhook receiver) + conexão com Meta API
2. [ ] State machine da conversa (definir todos os estados e transições)
3. [ ] Serviço de Vision/OCR com vLLM — testar com receitas reais
4. [ ] Pipeline LangChain4j — extração de entidades + threshold de confiança
5. [ ] Lista de medicamentos controlados (filtro pré-LLM)
6. [ ] Integração com API de medicamentos da farmácia
7. [ ] Checkout Service (PIX via Mercado Pago / PagSeguro)
8. [ ] Handoff Service (redirecionamento para WhatsApp do atendente)
9. [ ] Evento RabbitMQ → notificação do operador
10. [ ] Testes de carga (múltiplas conversas simultâneas no vLLM)
11. [ ] Provisionamento de GPU on-premise

---

## Perfil do Time

- Desenvolvedor sênior, 15 anos de experiência
- Stack dominada: Java, Spring, TypeScript, Docker, K8s, Angular, Nginx, RabbitMQ, Redis
- Decisão de framework de orquestração: LangChain4j (Java) — evita pular de runtime
