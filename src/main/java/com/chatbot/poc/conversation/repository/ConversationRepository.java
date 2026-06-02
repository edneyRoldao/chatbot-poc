package com.chatbot.poc.conversation.repository;

import com.chatbot.poc.conversation.domain.ConversationSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class ConversationRepository {

    private static final String KEY_PREFIX = "session:";

    private final RedisTemplate<String, ConversationSession> redisTemplate;
    private final long sessionTtlMinutes;

    public ConversationRepository(
            RedisTemplate<String, ConversationSession> redisTemplate,
            @Value("${app.conversation.session-ttl-minutes}") long sessionTtlMinutes) {
        this.redisTemplate = redisTemplate;
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    public Optional<ConversationSession> findById(String sessionId) {
        ConversationSession session = redisTemplate.opsForValue().get(buildKey(sessionId));
        return Optional.ofNullable(session);
    }

    public void save(ConversationSession session) {
        redisTemplate.opsForValue().set(buildKey(session.getSessionId()), session, sessionTtlMinutes, TimeUnit.MINUTES);
    }

    public void deleteById(String sessionId) {
        redisTemplate.delete(buildKey(sessionId));
    }

    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
