package com.linkups.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

/**
 * Date Range Validator
 *
 * Validator implementation for the {@link ValidDateRange} annotation.
 * Validates that date/time ranges in request DTOs are logically valid.
 *
 * <p>Validation Rules:</p>
 * <ul>
 *   <li>If both startTime and endTime are present: startTime must be before endTime</li>
 *   <li>If isAllDay is true: times should represent full days</li>
 *   <li>Duration must be positive</li>
 * </ul>
 *
 * <p>The validator uses reflection to access startTime, endTime, and isAllDay
 * fields from the target object.</p>
 *
 * @see ValidDateRange
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }

        try {
            // Use reflection to get startTime and endTime fields
            LocalDateTime startTime = getFieldValue(value, "startTime", LocalDateTime.class);
            LocalDateTime endTime = getFieldValue(value, "endTime", LocalDateTime.class);

            // If either is null, skip validation (let @NotNull handle required fields)
            if (startTime == null || endTime == null) {
                return true;
            }

            // Validate that startTime is before endTime
            if (!startTime.isBefore(endTime)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Start time must be before end time"
                ).addConstraintViolation();
                return false;
            }

            // Check for all-day event validation if isAllDay field exists
            Boolean isAllDay = getFieldValue(value, "isAllDay", Boolean.class);
            if (Boolean.TRUE.equals(isAllDay)) {
                if (!isValidAllDayEvent(startTime, endTime)) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                            "All-day events must span full days (start at 00:00, end at 23:59)"
                    ).addConstraintViolation();
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            // If reflection fails, allow validation to pass
            // (fields might not exist in this particular DTO)
            return true;
        }
    }

    /**
     * Get field value using reflection
     */
    private <T> T getFieldValue(Object object, String fieldName, Class<T> type) {
        try {
            Field field = findField(object.getClass(), fieldName);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            Object value = field.get(object);
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Find field in class hierarchy
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Validate all-day event time range
     * All-day events should start at midnight (00:00) and end at 23:59
     */
    private boolean isValidAllDayEvent(LocalDateTime startTime, LocalDateTime endTime) {
        // Check if start time is at midnight (00:00:00)
        boolean startsAtMidnight = startTime.getHour() == 0 &&
                                   startTime.getMinute() == 0 &&
                                   startTime.getSecond() == 0;

        // Check if end time is at end of day (23:59:xx)
        boolean endsAtEndOfDay = endTime.getHour() == 23 &&
                                 endTime.getMinute() == 59;

        return startsAtMidnight && endsAtEndOfDay;
    }
}