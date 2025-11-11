package org.example.livef_chatservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.livef_chatservice.dto.ChatMessage;
import org.example.livef_chatservice.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat/send")
    public void handleChat(@Payload ChatMessage message, Principal principal) {
        if (principal == null) {
            log.error("❌ 인증되지 않은 사용자의 메시지 전송 시도");
            return;
        }

        // JwtStompInterceptor에서 설정한 Principal (username)
        String sender = principal.getName();
        message.setSender(sender);

        log.info("💬 [{}] Message received: {}", sender, message);

        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        chatService.publishMessage(message);
    }

    @MessageMapping("/chat/enter")
    public void handleEnter(@Payload ChatMessage message, Principal principal) {
        if (principal == null) {
            log.warn("❌ 비인증 사용자의 입장 시도");
            return;
        }

        String sender = principal.getName();

        ChatMessage enterMessage = ChatMessage.createEnterMessage(
                message.getLeagueId(), sender
        );
        chatService.publishMessage(enterMessage);

        log.info("✅ [{}] entered league [{}]", sender, message.getLeagueId());
    }

    @MessageMapping("/chat/leave")
    public void handleLeave(@Payload ChatMessage message, Principal principal) {
        String sender = (principal != null) ? principal.getName() : message.getSender();

        ChatMessage leaveMessage = ChatMessage.createLeaveMessage(
                message.getLeagueId(), sender
        );
        chatService.publishMessage(leaveMessage);

        log.info("🚪 [{}] left league [{}]", sender, message.getLeagueId());
    }
}
