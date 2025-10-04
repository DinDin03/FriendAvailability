package com.linkups.api.dto.response.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

/**
 * Message Response DTO
 *
 * Data Transfer Object representing a chat message with sender information
 * and metadata.
 *
 * <p>This DTO includes:</p>
 * <ul>
 *   <li>Message content and metadata (ID, type, timestamp)</li>
 *   <li>Sender information (ID, name)</li>
 *   <li>Room context (room ID)</li>
 * </ul>
 *
 * <p>Used for:</p>
 * <ul>
 *   <li>WebSocket message delivery (/topic/chat/{roomId})</li>
 *   <li>Message history retrieval</li>
 *   <li>Search results</li>
 *   <li>Unread message lists</li>
 * </ul>
 *
 * @see com.linkups.domain.entity.Message
 * @see MessageListResponseDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDto {

    /**
     * Unique identifier of the message
     */
    private Long id;

    /**
     * User ID of the message sender
     */
    private Long senderId;

    /**
     * Name of the message sender
     */
    private String senderName;

    /**
     * ID of the chat room this message belongs to
     */
    private Long roomId;

    /**
     * Content of the message
     */
    private String content;

    /**
     * Type of the message
     * <p>Values: "TEXT", "SYSTEM_MESSAGE", "IMAGE", "FILE"</p>
     */
    private String messageType;

    /**
     * When the message was sent
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    // Helper methods

    /**
     * Check if this is a text message
     */
    public boolean isTextMessage() {
        return "TEXT".equals(messageType);
    }

    /**
     * Check if this is a system message
     */
    public boolean isSystemMessage() {
        return "SYSTEM_MESSAGE".equals(messageType);
    }

    /**
     * Check if the message was sent by a specific user
     */
    public boolean isFromUser(Long userId) {
        return senderId != null && senderId.equals(userId);
    }

    /**
     * Check if the message is recent (sent within last 5 minutes)
     */
    public boolean isRecent() {
        if (sentAt == null) return false;
        return sentAt.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    /**
     * Get formatted timestamp for display
     * <p>Format: "yyyy-MM-dd HH:mm"</p>
     */
    public String getFormattedTime() {
        if (sentAt == null) return "";
        return sentAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /**
     * Get relative time description (e.g., "2 minutes ago", "1 hour ago")
     */
    public String getRelativeTime() {
        if (sentAt == null) return "Unknown time";

        Duration duration = Duration.between(sentAt, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        if (days < 7) return days + " day" + (days > 1 ? "s" : "") + " ago";

        return getFormattedTime();
    }

    /**
     * Get a short preview of the message content (max 50 characters)
     */
    public String getShortPreview() {
        if (content == null) return "";
        return content.length() > 50
            ? content.substring(0, 47) + "..."
            : content;
    }
}
