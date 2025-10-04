package com.linkups.api.dto.response.friend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Friendship Statistics DTO
 *
 * Response DTO for friendship statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipStatisticsDTO {

    /**
     * Total number of friends
     */
    private Long totalFriends;

    /**
     * Number of pending friend requests
     */
    private Long pendingRequests;

    /**
     * Number of sent friend requests
     */
    private Long sentRequests;

    /**
     * Number of mutual friends (if applicable)
     */
    private Long mutualFriends;
}