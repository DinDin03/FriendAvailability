package com.friendavailability.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for typing indicator responses to frontend
 * 
 * Broadcast to: /topic/chat/{roomId}/typing
 * Contains user info and typing status for UI display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingResponseDto {
    
    private Long userId;
    private String userName;
    private Long roomId;
    private boolean isTyping;
    private LocalDateTime timestamp;
}