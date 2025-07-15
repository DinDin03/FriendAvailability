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
public class ErrorMessageDto {

    private String error;
    private LocalDateTime timestamp;
    private String errorCode;
    private String details;
}