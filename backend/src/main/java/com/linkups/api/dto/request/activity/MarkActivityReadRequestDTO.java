package com.linkups.api.dto.request.activity;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mark Activity Read Request DTO
 *
 * Request DTO for marking an activity as read.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkActivityReadRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;
}