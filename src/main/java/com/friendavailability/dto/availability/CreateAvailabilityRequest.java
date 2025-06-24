package com.friendavailability.dto.availability;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAvailabilityRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private String title;
    private String description;
    private String location;
    private Boolean isBusy;
    private Boolean isAllDay;
    private Integer reminderMinutes;

    public CreateAvailabilityRequest(Long userId, LocalDateTime startTime, LocalDateTime endTime, String title) {
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
    }

    @Override
    public String toString() {
        return "CreateAvailabilityRequest{" +
                "userId=" + userId +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", title='" + title + '\'' +
                ", isBusy=" + isBusy +
                ", isAllDay=" + isAllDay +
                '}';
    }
}