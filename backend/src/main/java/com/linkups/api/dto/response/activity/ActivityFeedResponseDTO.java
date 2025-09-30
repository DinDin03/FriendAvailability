package com.linkups.api.dto.response.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Activity Feed Response DTO
 *
 * Response DTO for activity feeds with pagination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityFeedResponseDTO {

    /**
     * List of activities
     */
    private List<ActivityResponseDTO> activities;

    /**
     * Whether there are more activities available
     */
    private Boolean hasMore;

    /**
     * Cursor for next page (timestamp-based pagination)
     */
    private String nextCursor;

    /**
     * Total count of activities
     */
    private Long totalCount;
}