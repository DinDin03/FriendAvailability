package com.linkups.api.dto.request.circle;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Update Circle Request DTO
 *
 * Data Transfer Object for updating existing circle information.
 * Contains optional fields that can be updated for a circle.
 *
 * <p>All fields are optional - only provided fields will be updated.</p>
 *
 * <p>Validation Rules:</p>
 * <ul>
 *   <li>name - Optional, must be between 2 and 100 characters if provided</li>
 *   <li>description - Optional, cannot exceed 500 characters if provided</li>
 * </ul>
 *
 * <p>Example request:</p>
 * <pre>
 * {
 *   "name": "Updated Study Group",
 *   "description": "Now focusing on advanced algorithms"
 * }
 * </pre>
 *
 * @see com.linkups.domain.entity.Circle
 * @see com.linkups.api.mapper.CircleMapper
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCircleRequest {

    /**
     * New name for the circle (optional)
     * <p>Must be between 2 and 100 characters if provided</p>
     */
    @Size(min = 2, max = 100, message = "Circle name must be between 2 and 100 characters")
    private String name;

    /**
     * New description for the circle (optional)
     * <p>Cannot exceed 500 characters if provided</p>
     */
    @Size(max = 500, message = "Circle description cannot exceed 500 characters")
    private String description;
}