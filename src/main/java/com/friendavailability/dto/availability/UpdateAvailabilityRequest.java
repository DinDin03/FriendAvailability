package com.friendavailability.dto.availability;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UpdateAvailabilityRequest {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String title;
    private String description;
    private String location;
    private Boolean isBusy;
    private Boolean isAllDay;
    private Integer reminderMinutes;

    @Override
    public String toString() {
        return "UpdateAvailabilityRequest{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", title='" + title + '\'' +
                ", isBusy=" + isBusy +
                ", isAllDay=" + isAllDay +
                '}';
    }
}