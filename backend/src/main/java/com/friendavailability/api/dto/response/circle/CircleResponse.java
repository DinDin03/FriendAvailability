package com.friendavailability.dto.circle;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class CircleResponse {
    
    private Long id;
    private String name;
    private String description;
    private Long createdBy;
    private String createdByName;
    private Integer maxMembers;
    private Integer currentMemberCount;
    private String circleColor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private String userRole;  
    private Boolean canManageMembers;
    private Boolean canModifyCircle;
    private Boolean canDeleteCircle;
    
    private Boolean isActive;
    private Boolean isAtMaxCapacity;
}