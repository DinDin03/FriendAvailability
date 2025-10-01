package com.linkups.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Activity Entity - Represents user activities in the social feed
 *
 * This entity captures various types of activities that users can perform,
 * such as availability changes, profile updates, circle activities, and social interactions.
 * It supports a flexible data structure to accommodate different activity types.
 */
@Entity
@Table(name = "activities", indexes = {
    @Index(name = "idx_activity_user_timestamp", columnList = "userId, timestamp"),
    @Index(name = "idx_activity_type", columnList = "type"),
    @Index(name = "idx_activity_timestamp", columnList = "timestamp"),
    @Index(name = "idx_activity_visibility", columnList = "visibility")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of activity - corresponds to ActivityType enum values
     * Examples: availability_change, profile_update, circle_activity, social_activity
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * ID of the user who performed the activity
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * Timestamp when the activity occurred
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * JSON data containing activity-specific information
     * Structure varies based on activity type
     */
    @Column(columnDefinition = "TEXT")
    private String data;

    /**
     * Priority level for the activity (1-5, where 5 is highest)
     * Used for sorting and highlighting important activities
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 1;

    /**
     * Visibility level of the activity
     * Values: public, friends, private
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String visibility = "friends";

    /**
     * Whether the activity is still active/relevant
     * Can be used to soft-delete or hide outdated activities
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Optional reference to a related entity (e.g., circle ID, event ID)
     */
    private Long relatedEntityId;

    /**
     * Type of the related entity (e.g., "circle", "event", "user")
     */
    @Column(length = 50)
    private String relatedEntityType;

    /**
     * Creation timestamp for auditing
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Last update timestamp for auditing
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Update the timestamp when the entity is modified
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Set creation timestamp when the entity is persisted
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.timestamp == null) {
            this.timestamp = now;
        }
    }
}