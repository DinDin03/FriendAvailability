package com.friendavailability.dto.circle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CreateCircleRequest {
    
    @NotBlank(message = "Circle name is required")
    @Size(min = 2, max = 100, message = "Circle name must be between 2 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Circle description cannot exceed 500 characters")
    private String description;
    
    @Min(value = 2, message = "Maximum members must be at least 2")
    private Integer maxMembers;
}