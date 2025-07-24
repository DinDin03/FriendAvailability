package com.friendavailability.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "availabilities",
        indexes = {
                @Index(name = "idx_availability_user_id", columnList = "user_id"),
                @Index(name = "idx_availability_start_time", columnList = "start_time"),
                @Index(name = "idx_availability_end_time", columnList = "end_time"),
                @Index(name = "idx_availability_time_range", columnList = "start_time, end_time")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Availability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "timezone", nullable = false, length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    @Builder.Default
    private AvailabilitySource source = AvailabilitySource.MANUAL;

    @Column(name = "google_event_id", length = 255)
    private String googleEventId;

    @Column(name = "is_busy", nullable = false)
    @Builder.Default
    private Boolean isBusy = false;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private Boolean isRecurring = false;

    @Column(name = "recurrence_rule", length = 255)
    private String recurrenceRule;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "location", length = 500)
    private String location;

    @Column(name = "is_all_day", nullable = false)
    @Builder.Default
    private Boolean isAllDay = false;

    @Column(name = "reminder_minutes")
    @Builder.Default
    private Integer reminderMinutes = 30;

    public Availability(User user, LocalDateTime startTime, LocalDateTime endTime, Boolean isBusy) {
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isBusy = isBusy;
        this.timezone = "UTC";
        this.source = AvailabilitySource.MANUAL;
        this.isRecurring = false;
        this.isAllDay = false;
        this.reminderMinutes = 30;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Availability(User user, LocalDateTime startTime, LocalDateTime endTime, Boolean isBusy, String title) {
        this(user, startTime, endTime, isBusy);
        this.title = title;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.timezone == null || this.timezone.trim().isEmpty()) {
            this.timezone = "UTC";
        }
        if (this.source == null) {
            this.source = AvailabilitySource.MANUAL;
        }
        if (this.isBusy == null) {
            this.isBusy = false;
        }
        if (this.isRecurring == null) {
            this.isRecurring = false;
        }
        if (this.isAllDay == null) {
            this.isAllDay = false;
        }
        if (this.reminderMinutes == null) {
            this.reminderMinutes = 30;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isValidTimeRange(){
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }

    public long getDurationInMinutes(){
        if(!isValidTimeRange()){
            return 0;
        }
        return java.time.Duration.between(startTime,endTime).toMinutes();
    }

    public boolean overlaps(Availability other) {
        if (other == null || !this.isValidTimeRange() || !other.isValidTimeRange()) {
            return false;
        }

        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }

    public boolean isInPast() {
        return endTime != null && endTime.isBefore(LocalDateTime.now());
    }

    public boolean isToday() {
        LocalDateTime now = LocalDateTime.now();
        return startTime.toLocalDate().equals(now.toLocalDate()) ||
                endTime.toLocalDate().equals(now.toLocalDate());
    }

    public boolean isValidAllDayEvent(){
        if(!isAllDay) return true;
        return startTime.getHour() == 0 &&
                startTime.getMinute() == 0 &&
                endTime.getHour() == 23 &&
                endTime.getMinute() == 59;
    }

    public boolean isMultiDay() {
        return !startTime.toLocalDate().equals(endTime.toLocalDate());
    }

    public boolean isOnDate(LocalDate date) {
        return startTime.toLocalDate().equals(date) ||
                endTime.toLocalDate().equals(date);
    }

    public long getDurationInHours() {
        return getDurationInMinutes() / 60;
    }

    public LocalDateTime getReminderTime() {
        if (reminderMinutes == null) return null;
        return startTime.minusMinutes(reminderMinutes);
    }

    @Override
    public String toString() {
        return "Availability{" +
                "id=" + id +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", timezone='" + timezone + '\'' +
                ", source=" + source +
                ", isBusy=" + isBusy +
                ", title='" + title + '\'' +
                ", location='" + location + '\'' +
                ", isAllDay=" + isAllDay +
                ", reminderMinutes=" + reminderMinutes +
                ", isRecurring=" + isRecurring +
                '}';
    }
}
