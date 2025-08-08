package com.friendavailability.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "used", nullable = false)
    @Builder.Default
    private boolean used = false;

    @PrePersist
    public void onCreate() {
        if(createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if(expiresAt == null) {
            expiresAt = LocalDateTime.now().plusHours(24);
        }
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    public void markAsUsed() {
        this.used = true;
    }

    public String getUserEmailForToken(){
        return user != null ? user.getEmail() : null;
    }

    @Override
    public String toString() {
        return "EmailVerificationToken{" +
                "id=" + id +
                ", userEmail='" + getUserEmailForToken() + '\'' +
                ", tokenLength=" + (token != null ? token.length() : 0) +
                ", expiresAt=" + expiresAt +
                ", used=" + used +
                ", expired=" + isExpired() +
                '}';
    }

}
