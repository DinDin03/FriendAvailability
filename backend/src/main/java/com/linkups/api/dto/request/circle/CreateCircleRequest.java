package com.linkups.api.dto.request.circle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Create Circle Request DTO
 *
 * Data Transfer Object for creating new social circles (groups).
 * Contains all required and optional information needed to create a new circle.
 *
 * <p>Validation Rules:</p>
 * <ul>
 *   <li>name - Required, must be between 2 and 100 characters</li>
 *   <li>description - Optional, cannot exceed 500 characters</li>
 *   <li>maxMembers - Optional, must be at least 2 if provided</li>
 * </ul>
 *
 * <p>Example request:</p>
 * <pre>
 * {
 *   "name": "Study Group",
 *   "description": "CS101 study group for final exams",
 *   "maxMembers": 10
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
public class CreateCircleRequest {

    /**
     * Name of the circle (required)
     * <p>Must be between 2 and 100 characters</p>
     * <p>Examples: "Study Group", "Weekend Hikers", "Book Club"</p>
     */
    @NotBlank(message = "Circle name is required")
    @Size(min = 2, max = 100, message = "Circle name must be between 2 and 100 characters")
    private String name;

    /**
     * Optional description of the circle's purpose
     * <p>Cannot exceed 500 characters</p>
     * <p>Example: "Weekly study sessions for Computer Science students"</p>
     */
    @Size(max = 500, message = "Circle description cannot exceed 500 characters")
    private String description;

    /**
     * Maximum number of members allowed in this circle (optional)
     * <p>Must be at least 2 if provided</p>
     * <p>null = unlimited members</p>
     * <p>Example: 10, 25, 50</p>
     */
    @Min(value = 2, message = "Maximum members must be at least 2")
    private Integer maxMembers;
}