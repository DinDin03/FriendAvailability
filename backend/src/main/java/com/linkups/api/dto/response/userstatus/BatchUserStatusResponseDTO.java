package com.linkups.api.dto.response.userstatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch User Status Response DTO
 *
 * Response DTO for batch user status requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUserStatusResponseDTO {

    /**
     * List of user statuses
     */
    private List<UserStatusResponseDTO> statuses;

    /**
     * Total count of statuses returned
     */
    private Integer count;
}