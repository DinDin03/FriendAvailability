package com.linkups.api.dto.request.circle;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferOwnershipRequest {
    
    @NotNull(message = "New owner user ID is required")
    private Long newOwnerId;
}