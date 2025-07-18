package com.friendavailability.dto.circle;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class CircleListResponse {
    
    private List<CircleResponse> circles;
    private Integer totalCount;
    
    private Integer circlesAsOwner;
    private Integer circlesAsAdmin;
    private Integer circlesAsMember;
}