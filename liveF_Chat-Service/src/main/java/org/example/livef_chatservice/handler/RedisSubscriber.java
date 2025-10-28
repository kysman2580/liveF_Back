package org.example.livef_chatservice.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.livef_chatservice.dto.ChatMessage;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Redis Pub/Sub 구독자
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            log.info("📩 Redis 수신: {}", body);

            ChatMessage chatMessage = objectMapper.readValue(body, ChatMessage.class);
            String destination = "/topic/league-" + chatMessage.getLeagueId();

            log.info("📤 WebSocket 전송: destination={}", destination);
            messagingTemplate.convertAndSend(destination, chatMessage);

        } catch (Exception e) {
            log.error("❌ 메시지 처리 실패: {}", e.getMessage(), e);
        }
    }
}