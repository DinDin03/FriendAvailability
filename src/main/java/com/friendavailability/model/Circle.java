package com.friendavailability.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "circles", indexes = {
        @Index(name = "idx_circle_created_by", columnList = "created_by"),
        @Index(name = "idx_circle_created_at", columnList = "created_at"),
        @Index(name = "idx_circle_is_active", columnList = "is_active"),
        @Index(name = "idx_circle_name", columnList = "name")
})
@Data
@Builder
public class Circle {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Circle name is required")
    @Size(min = 2, max = 100, message = "Circle bame must be between 2 and 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 100, message = "Circle description can not exceed 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "circle_color", length = 7)
    @Builder.Default
    private String circleColor = "#0052CC";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "circle", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CircleMember> members;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic methods
    public boolean isOwner(Long userId) {
        return this.createdBy.equals(userId);
    }

    public boolean isActiveCircle() {
        return this.isActive != null && this.isActive;
    }

    public boolean hasMaxMembers() {
        return this.maxMembers != null && this.maxMembers > 0;
    }

    public String getDisplayName() {
        return this.name != null ? this.name : "Unnamed Circle";
    }

    public int getMemberCount() {
        return this.members != null ? this.members.size() : 0;
    }

    public boolean isAtMaxCapacity() {
        if (!hasMaxMembers())
            return false;
        return getMemberCount() >= this.maxMembers;
    }
}
