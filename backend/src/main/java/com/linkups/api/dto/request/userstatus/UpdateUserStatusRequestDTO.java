package com.linkups.api.dto.request.userstatus;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update User Status Request DTO
 *
 * Request DTO for updating user status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequestDTO {

    @NotBlank(message = "Status is required")
    private String status;

    private String currentActivity;
}