package com.linkups.api.dto.request.userstatus;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch Status Request DTO
 *
 * Request DTO for getting statuses for multiple users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStatusRequestDTO {

    @NotEmpty(message = "User IDs list cannot be empty")
    private List<Long> userIds;
}