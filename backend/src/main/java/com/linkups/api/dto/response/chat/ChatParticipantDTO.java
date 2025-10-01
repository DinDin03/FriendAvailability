package com.linkups.api.dto.response.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Chat Participant DTO
 *
 * Data Transfer Object representing a participant in a chat room with their
 * role, status, and activity information.
 *
 * <p>This DTO includes:</p>
 * <ul>
 *   <li>Participant membership information (ID, role, join date)</li>
 *   <li>User information (ID, name, email)</li>
 *   <li>Activity status (online, active, last read)</li>
 *   <li>Permissions (admin status)</li>
 * </ul>
 *
 * <p>Used for:</p>
 * <ul>
 *   <li>GET /api/chat/rooms/{roomId}/participants - List of participants</li>
 *   <li>WebSocket user presence notifications</li>
 *   <li>Participant management in group chats</li>
 * </ul>
 *
 * @see com.linkups.domain.entity.ChatParticipant
 * @see ChatParticipantListResponseDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParticipantDTO {

    /**
     * Unique identifier of the participant record
     */
    private Long id;

    /**
     * User ID of the participant
     */
    private Long userId;

    /**
     * Name of the participant
     */
    private String userName;

    /**
     * Email of the participant
     */
    private String userEmail;

    /**
     * Role in the chat room
     * <p>Values: "ADMIN", "MEMBER"</p>
     */
    private String role;

    /**
     * When the participant joined the room
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime joinedAt;

    /**
     * Whether the participant is currently active in the room
     */
    private Boolean isActive;

    /**
     * Whether the participant is currently online
     * <p>Based on WebSocket connection status</p>
     */
    private Boolean isOnline;

    /**
     * When the participant last read messages
     * <p>null if never read or no messages</p>
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastReadAt;

    /**
     * Number of unread messages for this participant
     */
    private Long unreadCount;

    // Helper methods

    /**
     * Check if this participant is an admin
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /**
     * Check if this participant is a regular member
     */
    public boolean isMember() {
        return "MEMBER".equals(role);
    }

    /**
     * Check if the participant has unread messages
     */
    public boolean hasUnreadMessages() {
        return unreadCount != null && unreadCount > 0;
    }

    /**
     * Check if the participant is both active and online
     */
    public boolean isActiveAndOnline() {
        return Boolean.TRUE.equals(isActive) && Boolean.TRUE.equals(isOnline);
    }

    /**
     * Check if the participant recently read messages (within last hour)
     */
    public boolean hasRecentActivity() {
        if (lastReadAt == null) return false;
        return lastReadAt.isAfter(LocalDateTime.now().minusHours(1));
    }
}