package com.linkups.api.dto.response.circle;
import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class CircleMemberListResponse {

    private List<CircleMemberResponse> members;
    private Integer totalMembers;

    private Integer ownerCount;
    private Integer adminCount;
    private Integer memberCount;
}