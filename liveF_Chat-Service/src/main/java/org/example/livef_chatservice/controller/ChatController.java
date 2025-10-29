package org.example.livef_chatservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.livef_chatservice.dto.ChatMessage;
import org.example.livef_chatservice.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat/send")
    public void handleChat(@Payload ChatMessage message) {
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        chatService.publishMessage(message);
    }

    @MessageMapping("/chat/enter")
    public void handleEnter(@Payload ChatMessage message) {
        ChatMessage enterMessage = ChatMessage.createEnterMessage(
                message.getLeagueId(),
                message.getSender()
        );
        chatService.publishMessage(enterMessage);
    }

    @MessageMapping("/chat/leave")
    public void handleLeave(@Payload ChatMessage message) {
        ChatMessage leaveMessage = ChatMessage.createLeaveMessage(
                message.getLeagueId(),
                message.getSender()
        );
        chatService.publishMessage(leaveMessage);
    }
}