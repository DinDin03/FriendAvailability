package com.friendavailability.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages",
        indexes = {
                @Index(name = "idx_message_chat_room_id", columnList = "chat_room_id"),
                @Index(name = "idx_message_sender_id", columnList = "sender_id"),
                @Index(name = "idx_message_sent_at", columnList = "sent_at"),
                @Index(name = "idx_message_room_time", columnList = "chat_room_id, sent_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "sent_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    @JsonIgnore 
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @JsonIgnore 
    private User sender;

    @Column(name = "chat_room_id", insertable = false, updatable = false)
    private Long chatRoomId;

    @Column(name = "sender_id", insertable = false, updatable = false)
    private Long senderId;

    @PrePersist
    protected void onCreate() {
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }

    public boolean isTextMessage() {
        return this.messageType == MessageType.TEXT;
    }

    public boolean isSystemMessage() {
        return this.messageType == MessageType.SYSTEM_MESSAGE;
    }

    public String getSenderName() {
        return sender != null ? sender.getName() : "Unknown User";
    }

    public String getFormattedTime() {
        return sentAt != null ? sentAt.toString() : "";
    }
}