package com.friendavailability.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "circle_members",
        uniqueConstraints = {
                @UniqueConstraint(
                    name = "uk_circle_member_user_circle", 
                    columnNames = {"user_id", "circle_id"}
                )
        },
        indexes = {
                @Index(name = "idx_circle_member_user_id", columnList = "user_id"),
                @Index(name = "idx_circle_member_circle_id", columnList = "circle_id"),
                @Index(name = "idx_circle_member_role", columnList = "role"),
                @Index(name = "idx_circle_member_active", columnList = "is_active"),
                @Index(name = "idx_circle_member_joined_at", columnList = "joined_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private CircleRole role = CircleRole.MEMBER;

    @Column(name = "joined_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "invited_by")
    private Long invitedBy;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id", nullable = false)
    @NotNull(message = "Circle is required")
    @JsonIgnore
    private Circle circle;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "circle_id", insertable = false, updatable = false)
    private Long circleId;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.joinedAt == null) {
            this.joinedAt = now;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        this.lastActivityAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public boolean isOwner() {
        return this.role == CircleRole.OWNER;
    }

    public boolean isAdminOrOwner() {
        return this.role == CircleRole.OWNER || this.role == CircleRole.ADMIN;
    }

    public boolean isMember() {
        return this.role == CircleRole.MEMBER;
    }

    public boolean isActiveMember() {
        return this.isActive != null && this.isActive && this.leftAt == null;
    }

    public boolean canManageMembers() {
        return isActiveMember() && this.role.canManageMembers();
    }

   
    public boolean canModifyCircle() {
        return isActiveMember() && this.role.canModifyCircle();
    }

    public boolean canManageRoles() {
        return isActiveMember() && this.role.canManageRoles();
    }

    public boolean hasHigherPermissionThan(CircleMember otherMember) {
        if (otherMember == null || !isActiveMember()) return false;
        return this.role.hasHigherPermission(otherMember.role);
    }

    public void markAsLeft() {
        this.isActive = false;
        this.leftAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
    }

   
    public void reactivate() {
        this.isActive = true;
        this.leftAt = null;
        this.lastActivityAt = LocalDateTime.now();
    }

    public void updateRole(CircleRole newRole) {
        this.role = newRole;
        this.lastActivityAt = LocalDateTime.now();
    }

   
    public String getUserName() {
        return user != null ? user.getName() : "Unknown User";
    }

   
    public String getCircleName() {
        return circle != null ? circle.getName() : "Unknown Circle";
    }

    public String getRoleDisplayName() {
        return this.role != null ? this.role.getDisplayName() : "Unknown Role";
    }
}