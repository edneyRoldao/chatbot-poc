# Pharmacy Bot — Roadmap de Desenvolvimento

## Fase 1 — Fundação (Semanas 1–2)

- [ ] **Setup Meta WhatsApp Sandbox**
  - Criar app no developers.facebook.com (tipo Business)
  - Configurar Webhook + Verify Token
  - Cadastrar números de dev como testers
  - Instalar Cloudflare Tunnel para expor webhook local

- [ ] **Projeto NestJS — Webhook Receiver**
  - Receber e validar assinatura HMAC do Meta
  - Parser de payload (texto, imagem, documento, áudio)
  - Envio de mensagem de volta via API do Meta
  - Integração com Redis para session store
  - Estrutura de estados da conversa (state machine)

- [ ] **Projeto Spring Boot — Orquestrador**
  - Setup LangChain4j
  - Estrutura de casos de uso (receber receita, consultar med, checkout, handoff)
  - Comunicação com NestJS via HTTP interno

## Fase 2 — Inteligência (Semanas 3–5)

- [ ] **Vision Service / OCR**
  - Setup vLLM local (dev pode usar Ollama inicialmente)
  - Integrar Qwen2-VL-7B ou Llama 3.2 Vision
  - Prompt para extração de entidades da receita
  - Retorno de JSON estruturado com campo `confidence`
  - Testes com receitas reais digitadas e manuscritas em PT-BR

- [ ] **Filtro de Controlados**
  - Lista de princípios ativos C1, C2, antimicrobianos (lista ANVISA)
  - Aplicar ANTES de chamar o LLM (regex / lookup simples)
  - Trigger de handoff imediato se detectado

- [ ] **LLM de Diálogo**
  - Setup Llama 3.1 8B ou Mistral 7B via vLLM
  - Prompts para: saudação, confirmação de receita, apresentação de oferta, coleta de endereço
  - Contexto da conversa injetado a cada turno (via Redis)

## Fase 3 — Integrações (Semanas 6–8)

- [ ] **Integração API de Medicamentos**
  - Client HTTP (WebClient Spring) para API da farmácia
  - Lógica de busca por nome + dosagem
  - Tratamento de medicamento em falta → oferecer genérico ou handoff

- [ ] **Checkout Service**
  - Integração Mercado Pago ou PagSeguro
  - Geração de PIX com QR code e copia-e-cola
  - Geração de link de pagamento
  - Webhook de confirmação de pagamento

- [ ] **Handoff Service**
  - Envio de mensagem para número do atendente no WhatsApp
  - Payload: resumo do pedido, motivo do handoff, histórico da conversa
  - Timeout: se atendente não responder em X min → mensagem de status para cliente
  - Bot entra em modo SILENT após handoff

- [ ] **RabbitMQ — Eventos de Pedido**
  - Publicar evento `order.placed` com dados do pedido
  - Worker que consome e notifica operador
  - Dead letter queue para falhas de notificação

## Fase 4 — Qualidade e Compliance (Semana 9–10)

- [ ] **LGPD**
  - Garantir que imagem/PDF não é persistido em nenhum ponto
  - Audit log sem dados sensíveis de receita
  - Mascaramento do número de WhatsApp no banco se necessário

- [ ] **Testes**
  - Testes de integração dos fluxos principais
  - Testes de carga no vLLM (múltiplas conversas paralelas)
  - Teste de handoff em todos os cenários (controlado, OCR falho, erro)

- [ ] **Observabilidade**
  - Logs estruturados (sem dados sensíveis)
  - Métricas: taxa de handoff, tempo médio de atendimento, taxa de conversão
  - Health checks de todos os serviços

## Fase 5 — Produção (Semana 11–12)

- [ ] **Provisionamento de GPU on-premise**
  - Mínimo: NVIDIA A10G 24GB ou 2x RTX 4090
  - Setup vLLM em produção com ambos os modelos
  - Testes de performance em ambiente real

- [ ] **Número WhatsApp de Produção**
  - Verificação de conta Business no Meta
  - Portabilidade ou cadastro de número dedicado
  - Aprovação de templates de mensagem (se necessário)

- [ ] **Deploy K8s**
  - Manifests para todos os serviços
  - ConfigMaps e Secrets
  - HPA para NestJS e Spring Boot
  - PersistentVolume para PostgreSQL

---

## Decisões em Aberto

- [ ] Gateway de pagamento final: **Mercado Pago** vs **PagSeguro**
- [ ] Timeout de handoff: quantos minutos antes de avisar cliente?
- [ ] Política para medicamento em falta: oferecer genérico automaticamente ou sempre perguntar?
- [ ] Número do atendente: fixo ou round-robin entre atendentes disponíveis?
- [ ] Horário de funcionamento do bot: 24/7 ou apenas horário comercial?
