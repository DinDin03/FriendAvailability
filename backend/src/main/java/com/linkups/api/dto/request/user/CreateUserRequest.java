package com.linkups.api.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.*;

/**
 * Create User Request DTO
 *
 * Data Transfer Object for creating new user accounts.
 * Contains the required and optional information needed to register
 * a new user in the system.
 *
 * <p>This DTO supports both password-based and OAuth-based registration:</p>
 * <ul>
 *   <li>Password-based: name, email, and password are all provided</li>
 *   <li>OAuth-based: name and email are provided, password is null</li>
 * </ul>
 *
 * <p>Validation Rules:</p>
 * <ul>
 *   <li>name - Required, must not be blank</li>
 *   <li>email - Required, must not be blank, must be valid email format</li>
 *   <li>password - Optional, used for password-based accounts</li>
 * </ul>
 *
 * @see com.linkups.domain.entity.User
 * @see com.linkups.api.mapper.UserMapper
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    /**
     * User's full name (required)
     */
    @NotBlank(message = "Name is required")
    private String name;

    /**
     * User's email address (required, must be unique)
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    /**
     * User's password for password-based authentication (optional)
     * If null, user is assumed to be using OAuth authentication
     */
    private String password;

    @Override
    public String toString() {
        return "CreateUserRequest{name='" + name + "', email='" + email + "', hasPassword=" + (password != null) + "}";
    }
}