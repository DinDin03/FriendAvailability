package com.linkups.api.dto.response.chat;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Message List Response DTO
 *
 * Data Transfer Object for returning a list of messages with
 * pagination information and unread message statistics.
 *
 * <p>This DTO includes:</p>
 * <ul>
 *   <li>List of messages</li>
 *   <li>Pagination metadata (total, pages, current page)</li>
 *   <li>Unread message information</li>
 * </ul>
 *
 * <p>Used for:</p>
 * <ul>
 *   <li>GET /api/chat/rooms/{roomId}/messages - Get message history (paginated)</li>
 *   <li>GET /api/chat/rooms/{roomId}/messages/recent - Get recent messages</li>
 *   <li>GET /api/chat/rooms/{roomId}/messages/unread - Get unread messages</li>
 *   <li>GET /api/messages/search - Search messages</li>
 * </ul>
 *
 * @see MessageResponseDto
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageListResponseDTO {

    /**
     * List of messages
     */
    private List<MessageResponseDto> messages;

    /**
     * Total number of messages (across all pages)
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
     * Number of unread messages in this result set
     */
    private Long unreadCount;

    /**
     * Whether this list contains any unread messages
     */
    private Boolean hasUnread;

    // Helper methods

    /**
     * Check if the list is empty
     */
    public boolean isEmpty() {
        return messages == null || messages.isEmpty();
    }

    /**
     * Get the actual size of the messages list
     */
    public int getActualSize() {
        return messages != null ? messages.size() : 0;
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
        return Boolean.TRUE.equals(hasUnread) || (unreadCount != null && unreadCount > 0);
    }
}