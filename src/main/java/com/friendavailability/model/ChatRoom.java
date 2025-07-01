package com.friendavailability.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chat_rooms",
        indexes = {
                @Index(name = "idx_chat_room_created_by", columnList = "created_by"),
                @Index(name = "idx_chat_room_type", columnList = "type"),
                @Index(name = "idx_chat_room_created_at", columnList = "created_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Size(max = 100, message = "Chat room name cannot exceed 100 characters")
    @Column(name = "name", length = 100)
    private String name; 

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @Builder.Default
    private ChatType type = ChatType.PRIVATE;

    @Column(name = "created_by", nullable = false)
    private Long createdBy; 

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Message> messages;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ChatParticipant> participants;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPrivateChat() {
        return this.type == ChatType.PRIVATE;
    }

    public boolean isGroupChat() {
        return this.type == ChatType.GROUP;
    }

    public String getDisplayName() {
        return isPrivateChat() ? "Private Chat" : (name != null ? name : "Group Chat");
    }
}