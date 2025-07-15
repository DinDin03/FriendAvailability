package com.friendavailability.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMessageDto {
    
    private Long roomId;
    private String content;
    private String messageType;
    private LocalDateTime timestamp;
}