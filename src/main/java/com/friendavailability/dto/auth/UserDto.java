package com.friendavailability.dto.auth;

import com.friendavailability.model.User;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserDto {

    private Long id;
    private String email;
    private String name;
    private LocalDateTime createdAt;

    private boolean emailVerified;
    private boolean isActive;

    private String displayName;
    private boolean isOAuthUser;
    private boolean hasPassword;

    public static UserDto fromUser(User user) {
        if (user == null) {
            return null;
        }
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .emailVerified(user.getEmailVerified() != null ? user.getEmailVerified() : false)
                .isActive(user.getIsActive() != null ? user.getIsActive() : true)
                .displayName(user.getDisplayName())
                .isOAuthUser(user.isGoogleUser())
                .hasPassword(user.hasPassword())
                .build();
    }

    public static UserDto minimal(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }

    public static UserDto publicProfile(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .displayName(user.getDisplayName())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public boolean canLoginWithPassword() {
        return hasPassword && emailVerified && isActive;
    }

    public String getInitials() {
        if (name == null || name.trim().isEmpty()) {
            return email != null && !email.isEmpty() ?
                    email.substring(0, 1).toUpperCase() : "?";
        }

        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        } else {
            // Single name - take first 2 characters
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
    }

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

    public boolean isAccountComplete() {
        return name != null && !name.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                (hasPassword || isOAuthUser) &&
                isActive;
    }
}
