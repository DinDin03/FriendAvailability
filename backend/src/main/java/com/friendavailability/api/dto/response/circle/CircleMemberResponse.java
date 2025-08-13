package com.friendavailability.dto.circle;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class CircleMemberResponse {
    
    private Long membershipId;
    private Long userId;
    private String userName;
    private String userEmail;
    private String role;
    private String roleDisplayName;
    private LocalDateTime joinedAt;
    private Long invitedBy;
    private String invitedByName;
    
    private Boolean canPromote;
    private Boolean canRemove;
}