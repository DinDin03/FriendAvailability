package com.linkups.api.dto.request.activity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create Activity Request DTO
 *
 * Request DTO for creating a new activity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateActivityRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Activity type is required")
    private String type;

    private String data;

    @Builder.Default
    private Integer priority = 1;

    @Builder.Default
    private String visibility = "friends";

    private Long relatedEntityId;

    private String relatedEntityType;
}