package com.linkups.api.dto.request.availability;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.linkups.api.validation.ValidDateRange;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Create Availability Request DTO
 *
 * Data Transfer Object for creating new availability/calendar entries.
 * Contains all required and optional information needed to create a new
 * availability slot in the system.
 *
 * <p>Date/Time Format:</p>
 * All date/time fields must be provided in ISO-8601 format: {@code yyyy-MM-dd'T'HH:mm:ss}
 * <pre>
 * Example request:
 * {
 *   "userId": 1,
 *   "startTime": "2025-09-30T09:00:00",
 *   "endTime": "2025-09-30T17:00:00",
 *   "timezone": "America/New_York",
 *   "title": "Work Day",
 *   "isBusy": true
 * }
 * </pre>
 *
 * <p>Validation Rules:</p>
 * <ul>
 *   <li>userId - Required, must not be null</li>
 *   <li>startTime - Required, must not be null, must be before endTime</li>
 *   <li>endTime - Required, must not be null, must be after startTime</li>
 *   <li>timezone - Optional, defaults to "UTC" if not provided</li>
 *   <li>reminderMinutes - Optional, must be zero or positive if provided</li>
 *   <li>isAllDay - If true, times must represent full days (00:00 to 23:59)</li>
 * </ul>
 *
 * @see com.linkups.domain.entity.Availability
 * @see com.linkups.api.mapper.AvailabilityMapper
 * @see ValidDateRange
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidDateRange
public class CreateAvailabilityRequest {

    /**
     * ID of the user who owns this availability
     * Required field
     */
    @NotNull(message = "User ID is required")
    private Long userId;

    /**
     * Start date and time of the availability slot
     * <p>Format: ISO-8601 {@code yyyy-MM-dd'T'HH:mm:ss}</p>
     * <p>Example: {@code "2025-09-30T09:00:00"}</p>
     * <p>Must be before endTime</p>
     */
    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * End date and time of the availability slot
     * <p>Format: ISO-8601 {@code yyyy-MM-dd'T'HH:mm:ss}</p>
     * <p>Example: {@code "2025-09-30T17:00:00"}</p>
     * <p>Must be after startTime</p>
     */
    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * Timezone for the date/time fields
     * <p>Examples: "UTC", "America/New_York", "Europe/London", "Asia/Tokyo"</p>
     * <p>If not provided, defaults to "UTC"</p>
     */
    private String timezone;

    /**
     * Optional title/name for the availability slot
     */
    private String title;

    /**
     * Optional description of the availability slot
     */
    private String description;

    /**
     * Optional location where the event takes place
     */
    private String location;

    /**
     * Whether this time slot marks the user as busy
     * <p>true = user is unavailable during this time</p>
     * <p>false = user is free/available</p>
     * <p>Defaults to false if not provided</p>
     */
    private Boolean isBusy;

    /**
     * Whether this is an all-day event
     * <p>All-day events must start at 00:00:00 and end at 23:59:xx</p>
     * <p>Defaults to false if not provided</p>
     */
    private Boolean isAllDay;

    /**
     * Number of minutes before the event to send a reminder
     * <p>Must be zero or positive</p>
     * <p>null = no reminder</p>
     * <p>Common values: 15, 30, 60 (minutes)</p>
     */
    @PositiveOrZero(message = "Reminder minutes must be zero or positive")
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
                ", timezone='" + timezone + '\'' +
                ", title='" + title + '\'' +
                ", isBusy=" + isBusy +
                ", isAllDay=" + isAllDay +
                '}';
    }
}