package com.friendavailability.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email"),
                @UniqueConstraint(columnNames = "google_id")
        },
        indexes = {
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_google_id", columnList = "google_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "googleId", unique = true, length = 255)
    private String googleId;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Friend> friendships;


    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.emailVerified == null) {
            this.emailVerified = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isGoogleUser() {
        return this.googleId != null && !this.googleId.trim().isEmpty();
    }

    public boolean hasPassword() {
        return this.passwordHash != null && !this.passwordHash.trim().isEmpty();
    }

    public String getDisplayName() {
        return (name != null && !name.trim().isEmpty()) ? name : email;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", googleId='" + googleId + '\'' +
                ", isActive=" + isActive +
                ", emailVerified=" + emailVerified +
                ", createdAt=" + createdAt +
                '}';
    }
}