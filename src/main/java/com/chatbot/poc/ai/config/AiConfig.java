package com.chatbot.poc.ai.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.base-url}")
    private String baseUrl;

    @Value("${app.ai.model}")
    private String model;

    @Value("${app.ai.max-tokens}")
    private Integer maxTokens;

    @Value("${app.ai.temperature}")
    private Double temperature;

    @Bean
    public OpenAiChatModel chatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();
    }
}
