package com.friendavailability.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicatorDto {
    
    private Long userId;
    private Long roomId;
    private boolean isTyping;
}