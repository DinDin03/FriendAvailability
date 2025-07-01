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
@Table(name = "chat_participants",
        uniqueConstraints = {
                @UniqueConstraint(
                    name = "uk_chat_participant_user_room", 
                    columnNames = {"user_id", "chat_room_id"}
                )
        },
        indexes = {
                @Index(name = "idx_chat_participant_user_id", columnList = "user_id"),
                @Index(name = "idx_chat_participant_chat_room_id", columnList = "chat_room_id"),
                @Index(name = "idx_chat_participant_active", columnList = "is_active"),
                @Index(name = "idx_chat_participant_joined_at", columnList = "joined_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private ParticipantRole role = ParticipantRole.MEMBER;

    @Column(name = "joined_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    @NotNull(message = "Chat room is required")
    @JsonIgnore
    private ChatRoom chatRoom;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "chat_room_id", insertable = false, updatable = false)
    private Long chatRoomId;

    @PrePersist
    protected void onCreate() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    public boolean isMember() {
        return this.role == ParticipantRole.MEMBER;
    }

    public boolean isAdmin() {
        return this.role == ParticipantRole.ADMIN;
    }

    public boolean isActiveParticipant() {
        return this.isActive != null && this.isActive;
    }

    public String getUserName() {
        return user != null ? user.getName() : "Unknown User";
    }

    public String getUserEmail() {
        return user != null ? user.getEmail() : "Unknown Email";
    }

    public void leave() {
        this.isActive = false;
    }

    public void rejoin() {
        this.isActive = true;
    }

    public void promoteToAdmin() {
        this.role = ParticipantRole.ADMIN;
    }

    public void demoteToMember() {
        this.role = ParticipantRole.MEMBER;
    }
}