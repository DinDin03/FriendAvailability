package com.linkups.api.mapper;

import com.linkups.api.dto.request.availability.CreateAvailabilityRequest;
import com.linkups.api.dto.request.availability.UpdateAvailabilityRequest;
import com.linkups.api.dto.response.availability.AvailabilityResponse;
import com.linkups.api.dto.response.availability.TimeSlotDTO;
import com.linkups.domain.entity.Availability;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Availability Mapper
 *
 * Utility class for converting between Availability domain entities and DTOs.
 * Provides null-safe mapping methods to transform Availability entities into
 * AvailabilityResponse DTOs for API responses, and TimeSlotDTO for complex queries.
 *
 * <p>This mapper ensures:</p>
 * <ul>
 *   <li>Separation between internal domain model and external API contracts</li>
 *   <li>Sensitive data (Google Event IDs, recurrence rules, audit timestamps) is never exposed</li>
 *   <li>Null safety for all conversions</li>
 *   <li>Timezone information is properly transferred</li>
 *   <li>Consistent data transformation across the application</li>
 * </ul>
 *
 * @see Availability
 * @see AvailabilityResponse
 * @see TimeSlotDTO
 * @see CreateAvailabilityRequest
 * @see UpdateAvailabilityRequest
 */
public class AvailabilityMapper {

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private AvailabilityMapper() {
        throw new UnsupportedOperationException("AvailabilityMapper is a utility class and cannot be instantiated");
    }

    /**
     * Convert Availability entity to AvailabilityResponse DTO
     *
     * This method transforms an Availability domain entity into a safe AvailabilityResponse DTO
     * that can be exposed through the API. Internal fields like googleEventId, recurrence rules,
     * source, and audit timestamps are excluded.
     *
     * @param availability Availability entity to convert (can be null)
     * @return AvailabilityResponse DTO, or null if input availability is null
     */
    public static AvailabilityResponse toResponse(Availability availability) {
        if (availability == null) {
            return null;
        }

        // Extract userId from the User relationship
        Long userId = availability.getUser() != null ? availability.getUser().getId() : null;

        return AvailabilityResponse.builder()
                .id(availability.getId())
                .userId(userId)
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .timezone(availability.getTimezone() != null ? availability.getTimezone() : "UTC")
                .title(availability.getTitle())
                .description(availability.getDescription())
                .location(availability.getLocation())
                .isBusy(availability.getIsBusy() != null ? availability.getIsBusy() : false)
                .isAllDay(availability.getIsAllDay() != null ? availability.getIsAllDay() : false)
                .reminderMinutes(availability.getReminderMinutes())
                .build();
    }

    /**
     * Convert list of Availability entities to list of AvailabilityResponse DTOs
     *
     * Performs bulk conversion of Availability entities to DTOs. Filters out any
     * null availabilities from the input list.
     *
     * @param availabilities List of Availability entities to convert (can be null or contain nulls)
     * @return List of AvailabilityResponse DTOs (never null, but may be empty)
     */
    public static List<AvailabilityResponse> toResponseList(List<Availability> availabilities) {
        if (availabilities == null || availabilities.isEmpty()) {
            return Collections.emptyList();
        }

        return availabilities.stream()
                .filter(availability -> availability != null)
                .map(AvailabilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Availability entity to TimeSlotDTO
     *
     * Used for complex calendar operations like overlap detection and free slot calculations.
     * The slot type is set to BUSY if isBusy is true, otherwise FREE.
     *
     * @param availability Availability entity to convert (can be null)
     * @return TimeSlotDTO, or null if input availability is null
     */
    public static TimeSlotDTO toTimeSlot(Availability availability) {
        if (availability == null) {
            return null;
        }

        Long userId = availability.getUser() != null ? availability.getUser().getId() : null;
        boolean isBusy = availability.getIsBusy() != null ? availability.getIsBusy() : false;

        TimeSlotDTO.TimeSlotDTOBuilder builder = TimeSlotDTO.builder()
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .type(isBusy ? TimeSlotDTO.SlotType.BUSY : TimeSlotDTO.SlotType.FREE)
                .eventCount(1);

        if (userId != null) {
            builder.participantIds(List.of(userId));
        }

        if (availability.getTitle() != null) {
            builder.summary(availability.getTitle());
        }

        return builder.build();
    }

    /**
     * Convert list of Availability entities to list of TimeSlotDTO
     *
     * Performs bulk conversion for complex calendar queries like overlap detection.
     *
     * @param availabilities List of Availability entities to convert (can be null or contain nulls)
     * @return List of TimeSlotDTO (never null, but may be empty)
     */
    public static List<TimeSlotDTO> toTimeSlotList(List<Availability> availabilities) {
        if (availabilities == null || availabilities.isEmpty()) {
            return Collections.emptyList();
        }

        return availabilities.stream()
                .filter(availability -> availability != null)
                .map(AvailabilityMapper::toTimeSlot)
                .collect(Collectors.toList());
    }

    /**
     * Update existing Availability entity from UpdateAvailabilityRequest
     *
     * Applies changes from an UpdateAvailabilityRequest DTO to an existing Availability entity.
     * Only updates non-null fields from the request. Preserves all other entity fields.
     *
     * @param availability Availability entity to update (must not be null)
     * @param request UpdateAvailabilityRequest containing changes (must not be null)
     * @throws IllegalArgumentException if availability or request is null
     */
    public static void updateEntityFromRequest(Availability availability, UpdateAvailabilityRequest request) {
        if (availability == null) {
            throw new IllegalArgumentException("Availability entity cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("UpdateAvailabilityRequest cannot be null");
        }

        // Update date/time fields
        if (request.getStartTime() != null) {
            availability.setStartTime(request.getStartTime());
        }

        if (request.getEndTime() != null) {
            availability.setEndTime(request.getEndTime());
        }

        // Update timezone if provided
        if (request.getTimezone() != null && !request.getTimezone().trim().isEmpty()) {
            availability.setTimezone(request.getTimezone().trim());
        }

        // Update optional text fields
        if (request.getTitle() != null) {
            availability.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            availability.setDescription(request.getDescription());
        }

        if (request.getLocation() != null) {
            availability.setLocation(request.getLocation());
        }

        // Update boolean flags
        if (request.getIsBusy() != null) {
            availability.setIsBusy(request.getIsBusy());
        }

        if (request.getIsAllDay() != null) {
            availability.setIsAllDay(request.getIsAllDay());
        }

        // Update reminder
        if (request.getReminderMinutes() != null) {
            availability.setReminderMinutes(request.getReminderMinutes());
        }
    }

    /**
     * Create minimal AvailabilityResponse with only essential fields
     *
     * Used for situations where only basic availability information is needed,
     * such as in compact calendar views or lists.
     *
     * @param availability Availability entity to convert (can be null)
     * @return Minimal AvailabilityResponse with id, userId, startTime, endTime, and isBusy only
     */
    public static AvailabilityResponse toMinimalResponse(Availability availability) {
        if (availability == null) {
            return null;
        }

        Long userId = availability.getUser() != null ? availability.getUser().getId() : null;

        return AvailabilityResponse.builder()
                .id(availability.getId())
                .userId(userId)
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .isBusy(availability.getIsBusy() != null ? availability.getIsBusy() : false)
                .build();
    }
}