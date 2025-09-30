package com.linkups.api.dto.response.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Chat Room Response DTO
 *
 * Data Transfer Object representing a chat room with full details including
 * participant statistics and last message information.
 *
 * <p>This DTO includes:</p>
 * <ul>
 *   <li>Basic room information (name, type, metadata)</li>
 *   <li>Participant statistics (count, who created it)</li>
 *   <li>Last message preview (content, timestamp)</li>
 *   <li>User-specific information (unread count, member status)</li>
 * </ul>
 *
 * <p>Used for:</p>
 * <ul>
 *   <li>GET /api/chat/rooms/{roomId} - Single room details</li>
 *   <li>POST /api/chat/rooms/group - After creating group</li>
 *   <li>POST /api/chat/rooms/private - After creating/getting private chat</li>
 * </ul>
 *
 * @see com.linkups.domain.entity.ChatRoom
 * @see ChatRoomSummaryDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponseDTO {

    /**
     * Unique identifier of the chat room
     */
    private Long id;

    /**
     * Name of the chat room
     * <p>null for private chats (use display name instead)</p>
     */
    private String name;

    /**
     * Display name for UI
     * <p>For private chats: "Private Chat"</p>
     * <p>For group chats: the group name or "Group Chat"</p>
     */
    private String displayName;

    /**
     * Type of chat room
     * <p>Values: "PRIVATE", "GROUP"</p>
     */
    private String type;

    /**
     * User ID of the room creator
     */
    private Long createdBy;

    /**
     * Name of the user who created the room
     */
    private String createdByName;

    /**
     * When the room was created
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * When the room was last updated
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Number of active participants in the room
     */
    private Integer participantCount;

    /**
     * Timestamp of the most recent message
     * <p>null if no messages yet</p>
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastMessageAt;

    /**
     * Content of the most recent message (truncated to 100 chars)
     * <p>null if no messages yet</p>
     */
    private String lastMessageContent;

    /**
     * Number of unread messages for the requesting user
     */
    private Long unreadCount;

    /**
     * Whether the requesting user is an active participant
     */
    private Boolean isParticipant;

    /**
     * Role of the requesting user in this room
     * <p>Values: "ADMIN", "MEMBER"</p>
     * <p>null if user is not a participant</p>
     */
    private String userRole;

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
     * Check if there has been recent activity (message in last 24 hours)
     */
    public boolean hasRecentActivity() {
        if (lastMessageAt == null) return false;
        return lastMessageAt.isAfter(LocalDateTime.now().minusHours(24));
    }

    /**
     * Check if the requesting user is an admin
     */
    public boolean isAdmin() {
        return "ADMIN".equals(userRole);
    }

    /**
     * Check if the room is empty (no messages)
     */
    public boolean isEmpty() {
        return lastMessageAt == null;
    }

    /**
     * Get a short preview of the last message (max 50 chars)
     */
    public String getShortPreview() {
        if (lastMessageContent == null) return "No messages yet";
        return lastMessageContent.length() > 50
            ? lastMessageContent.substring(0, 47) + "..."
            : lastMessageContent;
    }
}