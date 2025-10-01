package com.linkups.api.dto.response.friend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Friend List Response DTO
 *
 * Response DTO for paginated friend lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendListResponseDTO {

    /**
     * List of friends
     */
    private List<FriendResponseDTO> friends;

    /**
     * Total count of friends
     */
    private Integer totalCount;

    /**
     * Current page number
     */
    private Integer page;

    /**
     * Page size/limit
     */
    private Integer limit;

    /**
     * Total number of pages
     */
    private Integer totalPages;
}