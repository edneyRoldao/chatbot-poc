package com.chatbot.poc.messaging.component;

import com.chatbot.poc.messaging.service.MessagingProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MessagingProviderRegistry {

    private final Map<String, MessagingProvider> providers;

    public MessagingProviderRegistry(List<MessagingProvider> providerList) {
        this.providers = providerList.stream()
            .collect(Collectors.toMap(MessagingProvider::providerName, Function.identity()));
    }

    public MessagingProvider get(String providerName) {
        MessagingProvider provider = providers.get(providerName);
        if (provider == null) throw new IllegalArgumentException("Unknown provider: " + providerName);
        return provider;
    }
}
