package com.linkups.api.dto.request.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message Request DTO
 *
 * Used for sending new messages via REST API.
 * For WebSocket messages, ChatMessageDto is still used.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequestDTO {

    @NotNull(message = "Sender ID is required")
    private Long senderId;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotBlank(message = "Message content cannot be empty")
    private String content;
}