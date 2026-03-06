package org.example.livef_chatservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.livef_chatservice.dto.ChatMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 채팅 서비스 (구현체만 사용)
 *
 * 역할:
 * - Redis Pub/Sub에 메시지 발행
 *
 * 💡 인터페이스 없이 구현체만 사용하는 이유:
 * 1. 현재 Redis만 사용 (다른 구현체 계획 없음)
 * 2. 필요 시 30초 리팩토링으로 인터페이스 분리 가능
 * 3. 코드 간결성 & 유지보수성 향상
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 메시지 발행
     *
     * @param message 발행할 메시지
     */
    public void publishMessage(ChatMessage message) {
        try {
            if (message.getTimestamp() == null) {
                message.setTimestamp(LocalDateTime.now());
            }

            String channel = "chat:league:" + message.getLeagueId();
            String jsonMessage = objectMapper.writeValueAsString(message);

            log.info("📢 Redis 발행: channel={}, sender={}, message={}",
                    channel, message.getSender(), message.getMessage());

            redisTemplate.convertAndSend(channel, jsonMessage);

        } catch (JsonProcessingException e) {
            log.error("❌ 메시지 발행 실패: {}", e.getMessage(), e);
            throw new RuntimeException("메시지 발행 실패", e);
        }
    }
}
