package com.linkups.api.dto.request.availability;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.linkups.api.validation.ValidDateRange;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Update Availability Request DTO
 *
 * Data Transfer Object for updating existing availability/calendar entries.
 * Contains optional fields that can be updated for an availability slot.
 *
 * <p>All fields are optional - only provided fields will be updated.
 * The service layer handles validation and ensures that changes are valid.</p>
 *
 * <p>Date/Time Format:</p>
 * All date/time fields must be provided in ISO-8601 format: {@code yyyy-MM-dd'T'HH:mm:ss}
 * <pre>
 * Example request:
 * {
 *   "startTime": "2025-09-30T10:00:00",
 *   "endTime": "2025-09-30T18:00:00",
 *   "title": "Updated Work Day",
 *   "isBusy": true
 * }
 * </pre>
 *
 * <p>Validation Rules:</p>
 * <ul>
 *   <li>If both startTime and endTime are provided: startTime must be before endTime</li>
 *   <li>timezone - Optional, can be changed to different timezone</li>
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
public class UpdateAvailabilityRequest {

    /**
     * Start date and time of the availability slot (optional)
     * <p>Format: ISO-8601 {@code yyyy-MM-dd'T'HH:mm:ss}</p>
     * <p>Example: {@code "2025-09-30T09:00:00"}</p>
     * <p>If provided with endTime, must be before endTime</p>
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * End date and time of the availability slot (optional)
     * <p>Format: ISO-8601 {@code yyyy-MM-dd'T'HH:mm:ss}</p>
     * <p>Example: {@code "2025-09-30T17:00:00"}</p>
     * <p>If provided with startTime, must be after startTime</p>
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * Timezone for the date/time fields (optional)
     * <p>Examples: "UTC", "America/New_York", "Europe/London", "Asia/Tokyo"</p>
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
     * Whether this time slot marks the user as busy (optional)
     * <p>true = user is unavailable during this time</p>
     * <p>false = user is free/available</p>
     */
    private Boolean isBusy;

    /**
     * Whether this is an all-day event (optional)
     * <p>All-day events must start at 00:00:00 and end at 23:59:xx</p>
     */
    private Boolean isAllDay;

    /**
     * Number of minutes before the event to send a reminder (optional)
     * <p>Must be zero or positive if provided</p>
     * <p>null = no reminder</p>
     * <p>Common values: 15, 30, 60 (minutes)</p>
     */
    @PositiveOrZero(message = "Reminder minutes must be zero or positive")
    private Integer reminderMinutes;

    @Override
    public String toString() {
        return "UpdateAvailabilityRequest{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", timezone='" + timezone + '\'' +
                ", title='" + title + '\'' +
                ", isBusy=" + isBusy +
                ", isAllDay=" + isAllDay +
                '}';
    }
}