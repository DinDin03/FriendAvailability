package com.linkups.api.mapper;

import com.linkups.api.dto.request.user.CreateUserRequest;
import com.linkups.api.dto.request.user.UpdateUserRequest;
import com.linkups.api.dto.response.user.UserResponse;
import com.linkups.domain.entity.User;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User Mapper
 *
 * Utility class for converting between User domain entities and DTOs.
 * Provides null-safe mapping methods to transform User entities into
 * UserResponse DTOs for API responses, and request DTOs into entities.
 *
 * <p>This mapper ensures:</p>
 * <ul>
 *   <li>Separation between internal domain model and external API contracts</li>
 *   <li>Sensitive data (passwords, relationships) is never exposed</li>
 *   <li>Null safety for all conversions</li>
 *   <li>Consistent data transformation across the application</li>
 * </ul>
 *
 * @see User
 * @see UserResponse
 * @see CreateUserRequest
 * @see UpdateUserRequest
 */
public class UserMapper {

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private UserMapper() {
        throw new UnsupportedOperationException("UserMapper is a utility class and cannot be instantiated");
    }

    /**
     * Convert User entity to UserResponse DTO
     *
     * This method transforms a User domain entity into a safe UserResponse DTO
     * that can be exposed through the API. Sensitive fields like passwordHash
     * and relationships are excluded.
     *
     * @param user User entity to convert (can be null)
     * @return UserResponse DTO, or null if input user is null
     */
    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .emailVerified(user.getEmailVerified() != null ? user.getEmailVerified() : false)
                .isActive(user.getIsActive() != null ? user.getIsActive() : true)
                .isOAuthUser(user.isGoogleUser())
                .hasPassword(user.hasPassword())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Convert list of User entities to list of UserResponse DTOs
     *
     * Performs bulk conversion of User entities to DTOs. Filters out any
     * null users from the input list.
     *
     * @param users List of User entities to convert (can be null or contain nulls)
     * @return List of UserResponse DTOs (never null, but may be empty)
     */
    public static List<UserResponse> toResponseList(List<User> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        return users.stream()
                .filter(user -> user != null)
                .map(UserMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create minimal UserResponse with only essential fields
     *
     * Used for situations where only basic user information is needed,
     * such as in lists or references within other DTOs.
     *
     * @param user User entity to convert (can be null)
     * @return Minimal UserResponse with id, name, email, and displayName only
     */
    public static UserResponse toMinimalResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }

    /**
     * Create public profile UserResponse
     *
     * Returns only publicly visible user information, suitable for
     * displaying to other users. Excludes sensitive flags like emailVerified.
     *
     * @param user User entity to convert (can be null)
     * @return Public UserResponse with id, name, displayName, and createdAt only
     */
    public static UserResponse toPublicProfile(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .displayName(user.getDisplayName())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Update existing User entity from UpdateUserRequest
     *
     * Applies changes from an UpdateUserRequest DTO to an existing User entity.
     * Only updates non-null fields from the request.
     *
     * @param user User entity to update (must not be null)
     * @param request UpdateUserRequest containing changes (must not be null)
     * @throws IllegalArgumentException if user or request is null
     */
    public static void updateEntityFromRequest(User user, UpdateUserRequest request) {
        if (user == null) {
            throw new IllegalArgumentException("User entity cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("UpdateUserRequest cannot be null");
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            user.setEmail(request.getEmail().trim().toLowerCase());
        }
    }

    /**
     * Create User entity from CreateUserRequest
     *
     * Constructs a new User entity from a CreateUserRequest DTO.
     * This is a partial entity creation - additional fields like
     * passwordHash should be set separately by the service layer.
     *
     * @param request CreateUserRequest with user data (must not be null)
     * @return New User entity with basic fields set
     * @throws IllegalArgumentException if request is null
     */
    public static User toEntity(CreateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateUserRequest cannot be null");
        }

        return User.builder()
                .name(request.getName() != null ? request.getName().trim() : null)
                .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
                .isActive(true)
                .emailVerified(false)
                .build();
    }
}