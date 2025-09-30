package com.linkups.api.dto.response.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Chat Room Summary DTO
 *
 * Lightweight Data Transfer Object for chat room list views.
 * Contains only essential information without full details.
 *
 * <p>This DTO is optimized for:</p>
 * <ul>
 *   <li>Chat room lists with many rooms</li>
 *   <li>Quick overviews in sidebars/navigation</li>
 *   <li>Real-time room lists</li>
 * </ul>
 *
 * <p>Comparison with ChatRoomResponseDTO:</p>
 * <ul>
 *   <li>ChatRoomSummaryDTO - Minimal fields for lists (faster, smaller payload)</li>
 *   <li>ChatRoomResponseDTO - Complete details for single room view</li>
 * </ul>
 *
 * @see ChatRoomResponseDTO
 * @see ChatRoomListResponseDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomSummaryDTO {

    /**
     * Unique identifier of the chat room
     */
    private Long id;

    /**
     * Display name for the room
     */
    private String displayName;

    /**
     * Type of chat room
     * <p>Values: "PRIVATE", "GROUP"</p>
     */
    private String type;

    /**
     * Number of active participants
     */
    private Integer participantCount;

    /**
     * Timestamp of the most recent message
     * <p>null if no messages yet</p>
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastMessageAt;

    /**
     * Preview of the last message (truncated)
     * <p>null if no messages yet</p>
     */
    private String lastMessagePreview;

    /**
     * Number of unread messages for the user
     */
    private Long unreadCount;

    /**
     * Whether the user is currently online/active in this room
     */
    private Boolean isActive;

    // Helper methods

    /**
     * Check if this is a private chat
     */
    public boolean isPrivateChat() {
        return "PRIVATE".equals(type);
    }

    /**
     * Check if this is a group chat
     */
    public boolean isGroupChat() {
        return "GROUP".equals(type);
    }

    /**
     * Check if there are unread messages
     */
    public boolean hasUnreadMessages() {
        return unreadCount != null && unreadCount > 0;
    }

    /**
     * Check if there has been recent activity (message in last hour)
     */
    public boolean hasRecentActivity() {
        if (lastMessageAt == null) return false;
        return lastMessageAt.isAfter(LocalDateTime.now().minusHours(1));
    }
}