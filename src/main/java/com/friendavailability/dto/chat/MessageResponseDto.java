package com.friendavailability.dto.chat;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private Long roomId;
    private String content;
    private String messageType;
    private LocalDateTime sentAt;
}
