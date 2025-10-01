package com.linkups.api.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User Response DTO
 *
 * Data Transfer Object for User API responses.
 * This DTO represents the external API contract and contains only
 * safe, non-sensitive user information that can be exposed to clients.
 *
 * <p>Purpose: Separate internal domain entities from external API contracts,
 * ensuring sensitive data like passwords and internal relationships are never
 * exposed through the API.</p>
 *
 * <p>Fields included:</p>
 * <ul>
 *   <li>id - Unique user identifier</li>
 *   <li>name - User's full name</li>
 *   <li>email - User's email address</li>
 *   <li>displayName - Display name (defaults to name or email)</li>
 *   <li>emailVerified - Whether email has been verified</li>
 *   <li>isActive - Whether account is active</li>
 *   <li>isOAuthUser - Whether user signed up via OAuth (Google)</li>
 *   <li>hasPassword - Whether user has password-based login enabled</li>
 *   <li>createdAt - Account creation timestamp</li>
 * </ul>
 *
 * @see com.linkups.domain.entity.User
 * @see com.linkups.api.mapper.UserMapper
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    /**
     * Unique identifier for the user
     */
    private Long id;

    /**
     * User's full name
     */
    private String name;

    /**
     * User's email address
     */
    private String email;

    /**
     * Display name used in UI (falls back to name or email)
     */
    private String displayName;

    /**
     * Whether the user's email address has been verified
     */
    private boolean emailVerified;

    /**
     * Whether the user account is active
     */
    private boolean isActive;

    /**
     * Whether the user registered via OAuth (Google)
     */
    private boolean isOAuthUser;

    /**
     * Whether the user has a password set for password-based login
     */
    private boolean hasPassword;

    /**
     * Timestamp when the user account was created
     */
    private LocalDateTime createdAt;

    /**
     * Get user's initials from name or email
     *
     * @return Two-character initials in uppercase
     */
    public String getInitials() {
        if (name == null || name.trim().isEmpty()) {
            return email != null && !email.isEmpty() ?
                    email.substring(0, 1).toUpperCase() : "?";
        }

        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        } else {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
    }

    /**
     * Get account type description based on authentication methods
     *
     * @return Human-readable account type (e.g., "Google Account", "Email Account")
     */
    public String getAccountType() {
        if (isOAuthUser && hasPassword) {
            return "Google + Password";
        } else if (isOAuthUser) {
            return "Google Account";
        } else if (hasPassword) {
            return "Email Account";
        } else {
            return "Incomplete Account";
        }
    }

    /**
     * Check if user can login with password
     *
     * @return true if user has password, verified email, and active account
     */
    public boolean canLoginWithPassword() {
        return hasPassword && emailVerified && isActive;
    }

    /**
     * Check if account setup is complete
     *
     * @return true if account has all required information and is active
     */
    public boolean isAccountComplete() {
        return name != null && !name.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                (hasPassword || isOAuthUser) &&
                isActive;
    }
}