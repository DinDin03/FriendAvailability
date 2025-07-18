package com.friendavailability.dto.circle;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCircleRequest {
    
    @Size(min = 2, max = 100, message = "Circle name must be between 2 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Circle description cannot exceed 500 characters")
    private String description;
}