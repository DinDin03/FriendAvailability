package com.friendavailability.dto.circle;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMemberToCircleRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
}