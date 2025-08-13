package com.friendavailability.api.dto.response.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptDto {
    private Long userId;
    private Long roomId;
    private LocalDateTime readAt;
}