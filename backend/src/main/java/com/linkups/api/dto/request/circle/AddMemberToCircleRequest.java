package com.linkups.api.dto.request.circle;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMemberToCircleRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
}