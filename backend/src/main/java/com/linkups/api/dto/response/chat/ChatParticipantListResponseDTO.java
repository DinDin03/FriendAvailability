package com.linkups.api.dto.response.chat;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Chat Participant List Response DTO
 *
 * Data Transfer Object for returning a list of chat participants with
 * aggregate statistics about role distribution and activity.
 *
 * <p>This DTO includes:</p>
 * <ul>
 *   <li>List of participants with their details</li>
 *   <li>Total participant count</li>
 *   <li>Breakdown by role (admin, member)</li>
 *   <li>Activity statistics (online, active counts)</li>
 * </ul>
 *
 * <p>Used for:</p>
 * <ul>
 *   <li>GET /api/chat/rooms/{roomId}/participants - Get all participants in a room</li>
 * </ul>
 *
 * @see ChatParticipantDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParticipantListResponseDTO {

    /**
     * List of participants in the chat room
     */
    private List<ChatParticipantDTO> participants;

    /**
     * Total number of participants
     */
    private Integer totalParticipants;

    /**
     * Number of participants with ADMIN role
     */
    private Integer adminCount;

    /**
     * Number of participants with MEMBER role
     */
    private Integer memberCount;

    /**
     * Number of participants currently online
     */
    private Integer onlineCount;

    /**
     * Number of participants who are active (not left the room)
     */
    private Integer activeCount;

    // Helper methods

    /**
     * Check if the list is empty
     */
    public boolean isEmpty() {
        return participants == null || participants.isEmpty();
    }

    /**
     * Get the actual size of the participants list
     */
    public int getActualSize() {
        return participants != null ? participants.size() : 0;
    }

    /**
     * Check if the room has any admins
     */
    public boolean hasAdmins() {
        return adminCount != null && adminCount > 0;
    }

    /**
     * Check if there are any online participants
     */
    public boolean hasOnlineUsers() {
        return onlineCount != null && onlineCount > 0;
    }

    /**
     * Get the percentage of online users
     */
    public double getOnlinePercentage() {
        if (totalParticipants == null || totalParticipants == 0 || onlineCount == null) {
            return 0.0;
        }
        return (onlineCount * 100.0) / totalParticipants;
    }
}