package com.linkups.api.dto.response.userstatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Status Statistics DTO
 *
 * Response DTO for user status statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusStatisticsDTO {

    /**
     * Number of online users
     */
    private Long onlineCount;

    /**
     * Number of away users
     */
    private Long awayCount;

    /**
     * Number of busy users
     */
    private Long busyCount;

    /**
     * Number of offline users
     */
    private Long offlineCount;

    /**
     * Total number of users with status
     */
    private Long totalUsers;
}