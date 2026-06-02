package com.chatbot.poc.messaging.component;

import com.chatbot.poc.conversation.service.ConversationService;
import com.chatbot.poc.messaging.domain.InboundMessage;
import com.chatbot.poc.messaging.domain.OutboundMessage;
import com.chatbot.poc.messaging.service.MessagingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConversationRouter {

    private static final Logger log = LoggerFactory.getLogger(ConversationRouter.class);

    private final MessagingProviderRegistry providerRegistry;
    private final ConversationService conversationService;

    public ConversationRouter(MessagingProviderRegistry providerRegistry, ConversationService conversationService) {
        this.providerRegistry = providerRegistry;
        this.conversationService = conversationService;
    }

    public void route(InboundMessage message) {
        log.info("Inbound [{}] session={} text=\"{}\"", message.providerName(), message.sessionId(), message.text());
        String reply = conversationService.handle(message);
        sendReply(message, reply);
        log.info("Outbound [{}] recipient={} text=\"{}\"", message.providerName(), message.senderId(), reply);
    }

    private void sendReply(InboundMessage message, String reply) {
        OutboundMessage outbound = new OutboundMessage(message.senderId(), reply);
        MessagingProvider provider = providerRegistry.get(message.providerName());
        provider.send(outbound);
    }

}
