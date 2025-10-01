package com.linkups.domain.entity;

import com.linkups.domain.entity.enums.UserStatusType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_status", indexes = {
        @Index(name = "idx_user_status_user_id", columnList = "userId"),
        @Index(name = "idx_user_status_status", columnList = "status"),
        @Index(name = "idx_user_status_last_seen", columnList = "lastSeen")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatusType status = UserStatusType.OFFLINE;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime lastSeen = LocalDateTime.now();

    @Column(length = 100)
    private String currentActivity;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status != UserStatusType.OFFLINE) {
            this.lastSeen = LocalDateTime.now();
        }
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastSeen = now;
    }


}
