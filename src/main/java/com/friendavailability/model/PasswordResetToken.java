package com.friendavailability.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "password_reset_token")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

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
    public void onCreate(){
        if(createdAt == null) createdAt = LocalDateTime.now();
        if(expiresAt == null) expiresAt = LocalDateTime.now().plusMinutes(30);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid(){
        return !used && !isExpired();
    }

    public String getUserEmailForToken(){
        return user != null ? user.getEmail() : null;
    }

    public String toString() {
        return "PasswordResetToken{" +
                "id=" + id +
                ", userEmail='" + getUserEmailForToken() + '\'' +
                ", tokenLength=" + (token != null ? token.length() : 0) +
                ", expiresAt=" + expiresAt +
                ", used=" + used +
                ", expired=" + isExpired() +
                '}';
    }

}
