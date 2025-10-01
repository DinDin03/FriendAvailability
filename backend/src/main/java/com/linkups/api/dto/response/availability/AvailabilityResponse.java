package com.linkups.api.dto.response.availability;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Availability Response DTO
 *
 * Data Transfer Object for Availability API responses.
 * This DTO represents the external API contract for calendar/scheduling data
 * and contains only safe, non-sensitive availability information.
 *
 * <p>Purpose: Separate internal domain entities from external API contracts,
 * ensuring internal implementation details (like Google Event IDs, recurrence rules,
 * or audit timestamps) are never exposed through the API.</p>
 *
 * <p>Date/Time Format:</p>
 * All date/time fields use ISO-8601 format: {@code yyyy-MM-dd'T'HH:mm:ss}
 * <pre>
 * Example: "2025-09-30T14:30:00"
 * </pre>
 *
 * <p>Timezone Handling:</p>
 * The {@code timezone} field indicates the timezone for the startTime and endTime.
 * Times are stored and transmitted in their local timezone representation.
 * Common values: "UTC", "America/New_York", "Europe/London", "Asia/Tokyo"
 *
 * <p>Fields included:</p>
 * <ul>
 *   <li>id - Unique availability identifier</li>
 *   <li>userId - ID of the user who owns this availability</li>
 *   <li>startTime - Start date/time of the availability slot</li>
 *   <li>endTime - End date/time of the availability slot</li>
 *   <li>timezone - Timezone for the date/time fields</li>
 *   <li>title - Optional event title</li>
 *   <li>description - Optional event description</li>
 *   <li>location - Optional event location</li>
 *   <li>isBusy - Whether this slot marks the user as busy</li>
 *   <li>isAllDay - Whether this is an all-day event</li>
 *   <li>reminderMinutes - Minutes before event to send reminder</li>
 * </ul>
 *
 * @see com.linkups.domain.entity.Availability
 * @see com.linkups.api.mapper.AvailabilityMapper
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {

    /**
     * Unique identifier for the availability slot
     */
    private Long id;

    /**
     * ID of the user who owns this availability
     */
    private Long userId;

    /**
     * Start date and time of the availability slot
     * <p>Format: ISO-8601 {@code yyyy-MM-dd'T'HH:mm:ss}</p>
     * <p>Example: {@code "2025-09-30T09:00:00"}</p>
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * End date and time of the availability slot
     * <p>Format: ISO-8601 {@code yyyy-MM-dd'T'HH:mm:ss}</p>
     * <p>Example: {@code "2025-09-30T17:00:00"}</p>
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * Timezone for the startTime and endTime
     * <p>Examples: "UTC", "America/New_York", "Europe/London"</p>
     * <p>Default: "UTC"</p>
     */
    private String timezone;

    /**
     * Optional title/name for the event
     */
    private String title;

    /**
     * Optional description of the event
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
     */
    private Boolean isBusy;

    /**
     * Whether this is an all-day event
     * <p>All-day events typically span from 00:00 to 23:59</p>
     */
    private Boolean isAllDay;

    /**
     * Number of minutes before the event to send a reminder
     * <p>null = no reminder</p>
     * <p>Common values: 15, 30, 60 (minutes)</p>
     */
    private Integer reminderMinutes;

    /**
     * Calculate the duration of this availability slot in minutes
     *
     * @return Duration in minutes, or 0 if time range is invalid
     */
    public long getDurationInMinutes() {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }

    /**
     * Calculate the duration of this availability slot in hours
     *
     * @return Duration in hours (rounded down), or 0 if time range is invalid
     */
    public long getDurationInHours() {
        return getDurationInMinutes() / 60;
    }

    /**
     * Check if this availability spans multiple days
     *
     * @return true if startTime and endTime are on different dates
     */
    public boolean isMultiDay() {
        if (startTime == null || endTime == null) {
            return false;
        }
        return !startTime.toLocalDate().equals(endTime.toLocalDate());
    }

    /**
     * Check if this availability is in the past
     *
     * @return true if endTime is before current time
     */
    public boolean isInPast() {
        return endTime != null && endTime.isBefore(LocalDateTime.now());
    }

    /**
     * Get the reminder time (when reminder should be sent)
     *
     * @return LocalDateTime of when to send reminder, or null if no reminder set
     */
    public LocalDateTime getReminderTime() {
        if (reminderMinutes == null || startTime == null) {
            return null;
        }
        return startTime.minusMinutes(reminderMinutes);
    }

    /**
     * Format duration as human-readable string
     *
     * @return Duration formatted as "Xh Ym" or "Xm"
     */
    public String getFormattedDuration() {
        long minutes = getDurationInMinutes();
        if (minutes == 0) return "0m";

        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;

        if (hours > 0 && remainingMinutes > 0) {
            return String.format("%dh %dm", hours, remainingMinutes);
        } else if (hours > 0) {
            return String.format("%dh", hours);
        } else {
            return String.format("%dm", remainingMinutes);
        }
    }
}