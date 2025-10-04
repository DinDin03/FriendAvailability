package com.linkups.api.dto.request.friend;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Accept Friend Request DTO
 *
 * Request DTO for accepting a friend request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptFriendRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;
}