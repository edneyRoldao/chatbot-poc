package com.chatbot.poc.shared.config;

import com.chatbot.poc.conversation.domain.ConversationSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ConversationSession> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        RedisTemplate<String, ConversationSession> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        JacksonJsonRedisSerializer<ConversationSession> serializer =
                new JacksonJsonRedisSerializer<>(objectMapper, ConversationSession.class);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(serializer);

        return template;
    }

}
