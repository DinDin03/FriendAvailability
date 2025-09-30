package com.linkups.api.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Update User Request DTO
 *
 * Data Transfer Object for updating existing user information.
 * Contains optional fields that can be updated for a user account.
 *
 * <p>All fields are optional - only provided fields will be updated.
 * The service layer handles validation of required constraints and
 * ensuring that changes are valid.</p>
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>name - User's full name (optional, must be 2-100 chars if provided)</li>
 *   <li>email - User's email address (optional, must be valid email format if provided)</li>
 * </ul>
 *
 * @see com.linkups.domain.entity.User
 * @see com.linkups.api.mapper.UserMapper
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    /**
     * User's full name (optional)
     * Must be between 2 and 100 characters if provided
     */
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    /**
     * User's email address (optional)
     * Must be a valid email format if provided
     */
    @Email(message = "Valid email is required")
    private String email;

    @Override
    public String toString() {
        return "UpdateUserRequest{name='" + name + "', email='" + email + "'}";
    }
}