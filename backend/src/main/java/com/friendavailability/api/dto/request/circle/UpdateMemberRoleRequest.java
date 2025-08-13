package com.friendavailability.api.dto.request.circle;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMemberRoleRequest {
    
    @NotBlank(message = "New role is required")
    private String newRole;  
}