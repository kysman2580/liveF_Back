package org.example.livef_chatservice.dto;

// src/main/java/.../domain/ChatMessage.java (DTO)

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 DTO
 *
 * 역할: 클라이언트 ↔ 서버 ↔ Redis 간 메시지 데이터 전달
 *
 * 🔄 변경: matchId → leagueId (리그별 채팅방)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChatMessage {

    /**
     * 메시지 타입
     */
    public enum MessageType {
        ENTER,    // 입장: "홍길동님이 입장하셨습니다."
        TALK,     // 일반 채팅
        LEAVE     // 퇴장: "홍길동님이 퇴장하셨습니다."
    }

    private MessageType type;          // 메시지 타입
    private Integer leagueId;          // 리그 ID (39=프리미어, 140=라리가, 135=세리에A)
    private String sender;             // 발신자 (사용자명)
    private String message;            // 메시지 내용

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss",
               timezone = "Asia/Seoul")
    private LocalDateTime timestamp;   // 전송 시각 (ISO-8601)

    /**
     * 입장 메시지 생성 헬퍼
     */
    public static ChatMessage createEnterMessage(Integer leagueId, String sender) {
        return ChatMessage.builder()
                .type(MessageType.ENTER)
                .leagueId(leagueId)
                .sender(sender)
                .message(sender + "님이 입장하셨습니다.")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 퇴장 메시지 생성 헬퍼
     */
    public static ChatMessage createLeaveMessage(Integer leagueId, String sender) {
        return ChatMessage.builder()
                .type(MessageType.LEAVE)
                .leagueId(leagueId)
                .sender(sender)
                .message(sender + "님이 퇴장하셨습니다.")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
