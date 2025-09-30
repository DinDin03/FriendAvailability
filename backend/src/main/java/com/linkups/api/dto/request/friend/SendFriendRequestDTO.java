package com.linkups.api.dto.request.friend;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Send Friend Request DTO
 *
 * Request DTO for sending a friend request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendFriendRequestDTO {

    @NotNull(message = "From user ID is required")
    private Long fromUserId;

    @NotNull(message = "To user ID is required")
    private Long toUserId;
}