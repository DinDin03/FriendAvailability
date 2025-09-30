package com.linkups.api.dto.response.availability;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Time Slot DTO
 *
 * Data Transfer Object representing a time slot in calendar queries.
 * Used for complex availability operations like:
 * <ul>
 *   <li>Finding overlapping availabilities between multiple users</li>
 *   <li>Calculating free time slots</li>
 *   <li>Identifying scheduling conflicts</li>
 *   <li>Aggregating availability across users or calendars</li>
 * </ul>
 *
 * <p>Use Cases:</p>
 * <pre>
 * 1. Overlap Detection:
 *    - Type: CONFLICT
 *    - Shows when multiple users have overlapping busy times
 *
 * 2. Free Slot Calculation:
 *    - Type: FREE
 *    - Shows when all participants are available
 *
 * 3. Busy Aggregation:
 *    - Type: BUSY
 *    - Shows cumulative busy times across calendars
 * </pre>
 *
 * <p>Date/Time Format:</p>
 * All date/time fields use ISO-8601 format: {@code yyyy-MM-dd'T'HH:mm:ss}
 *
 * @see AvailabilityResponse
 * @see com.linkups.api.mapper.AvailabilityMapper
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDTO {

    /**
     * Type of time slot
     */
    public enum SlotType {
        /**
         * Time slot where participants are free/available
         */
        FREE,

        /**
         * Time slot where one or more participants are busy
         */
        BUSY,

        /**
         * Time slot where multiple events/availabilities overlap
         * Used to highlight scheduling conflicts
         */
        CONFLICT
    }

    /**
     * Start date and time of the time slot
     * <p>Format: ISO-8601 {@code yyyy-MM-dd'T'HH:mm:ss}</p>
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * End date and time of the time slot
     * <p>Format: ISO-8601 {@code yyyy-MM-dd'T'HH:mm:ss}</p>
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * Type of time slot (FREE, BUSY, or CONFLICT)
     */
    private SlotType type;

    /**
     * IDs of users/participants associated with this time slot
     * <p>For CONFLICT slots: users with overlapping events</p>
     * <p>For BUSY slots: users who are busy during this time</p>
     * <p>For FREE slots: users who are available</p>
     */
    @Builder.Default
    private List<Long> participantIds = new ArrayList<>();

    /**
     * Number of events/availabilities in this time slot
     * <p>For CONFLICT slots: number of overlapping events</p>
     * <p>For other types: typically 1</p>
     */
    @Builder.Default
    private Integer eventCount = 0;

    /**
     * Optional description or summary of the time slot
     * <p>For CONFLICT: "3 events overlap"</p>
     * <p>For FREE: "All available"</p>
     */
    private String summary;

    /**
     * Calculate the duration of this time slot in minutes
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
     * Calculate the duration of this time slot in hours
     *
     * @return Duration in hours (rounded down)
     */
    public long getDurationInHours() {
        return getDurationInMinutes() / 60;
    }

    /**
     * Check if this time slot overlaps with another
     *
     * @param other Another TimeSlotDTO to check overlap with
     * @return true if the time slots overlap
     */
    public boolean overlaps(TimeSlotDTO other) {
        if (other == null || startTime == null || endTime == null ||
            other.startTime == null || other.endTime == null) {
            return false;
        }
        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }

    /**
     * Check if this time slot is in the past
     *
     * @return true if endTime is before current time
     */
    public boolean isInPast() {
        return endTime != null && endTime.isBefore(LocalDateTime.now());
    }

    /**
     * Check if this time slot is currently active
     *
     * @return true if current time is between startTime and endTime
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return startTime != null && endTime != null &&
               !startTime.isAfter(now) && !endTime.isBefore(now);
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

    /**
     * Get a concise summary of this time slot
     *
     * @return Human-readable summary
     */
    public String getAutoSummary() {
        if (summary != null && !summary.isEmpty()) {
            return summary;
        }

        switch (type) {
            case FREE:
                return participantIds.size() > 0 ?
                       String.format("%d available", participantIds.size()) :
                       "Free time";
            case BUSY:
                return participantIds.size() > 0 ?
                       String.format("%d busy", participantIds.size()) :
                       "Busy";
            case CONFLICT:
                return eventCount > 0 ?
                       String.format("%d events overlap", eventCount) :
                       "Scheduling conflict";
            default:
                return "Unknown";
        }
    }
}