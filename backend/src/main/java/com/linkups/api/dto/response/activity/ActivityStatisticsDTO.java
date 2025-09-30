package com.linkups.api.dto.response.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Activity Statistics DTO
 *
 * Response DTO for activity statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStatisticsDTO {

    /**
     * Total number of activities
     */
    private Long totalActivities;

    /**
     * Number of activities today
     */
    private Long todayActivities;

    /**
     * Number of activities this week
     */
    private Long weekActivities;

    /**
     * Last activity timestamp
     */
    private String lastUpdated;
}