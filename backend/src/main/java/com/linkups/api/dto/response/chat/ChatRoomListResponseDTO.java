package com.linkups.api.dto.response.chat;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Chat Room List Response DTO
 *
 * Data Transfer Object for returning a list of chat rooms with
 * pagination information and statistics.
 *
 * <p>This DTO includes:</p>
 * <ul>
 *   <li>List of chat room summaries</li>
 *   <li>Pagination metadata (total, pages, current page)</li>
 *   <li>Statistics (total rooms, unread counts)</li>
 * </ul>
 *
 * <p>Used for:</p>
 * <ul>
 *   <li>GET /api/chat/rooms - Get all user's chat rooms (paginated)</li>
 * </ul>
 *
 * @see ChatRoomSummaryDTO
 * @see ChatRoomResponseDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomListResponseDTO {

    /**
     * List of chat room summaries
     */
    private List<ChatRoomSummaryDTO> chatRooms;

    /**
     * Total number of chat rooms (across all pages)
     */
    private Long totalElements;

    /**
     * Total number of pages
     */
    private Integer totalPages;

    /**
     * Current page number (0-indexed)
     */
    private Integer currentPage;

    /**
     * Page size (number of items per page)
     */
    private Integer size;

    /**
     * Total number of unread messages across all rooms
     */
    private Long totalUnreadCount;

    // Helper methods

    /**
     * Check if the list is empty
     */
    public boolean isEmpty() {
        return chatRooms == null || chatRooms.isEmpty();
    }

    /**
     * Get the actual size of the rooms list
     */
    public int getActualSize() {
        return chatRooms != null ? chatRooms.size() : 0;
    }

    /**
     * Check if there are more pages available
     */
    public boolean hasMore() {
        if (totalPages == null || currentPage == null) return false;
        return currentPage < totalPages - 1;
    }

    /**
     * Check if this is the first page
     */
    public boolean isFirstPage() {
        return currentPage != null && currentPage == 0;
    }

    /**
     * Check if this is the last page
     */
    public boolean isLastPage() {
        if (totalPages == null || currentPage == null) return true;
        return currentPage >= totalPages - 1;
    }

    /**
     * Check if there are any unread messages
     */
    public boolean hasUnreadMessages() {
        return totalUnreadCount != null && totalUnreadCount > 0;
    }
}