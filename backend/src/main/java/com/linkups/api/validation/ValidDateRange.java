package com.linkups.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Valid Date Range Annotation
 *
 * Custom validation annotation to ensure that date/time ranges are valid:
 * <ul>
 *   <li>Start time must be before end time</li>
 *   <li>All-day events must span full days (00:00 to 23:59)</li>
 *   <li>Duration must be positive (no zero or negative durations)</li>
 * </ul>
 *
 * <p>This annotation should be applied at the class level on DTOs that
 * contain startTime and endTime fields.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code @ValidDateRange}
 * public class CreateAvailabilityRequest {
 *     private LocalDateTime startTime;
 *     private LocalDateTime endTime;
 *     private Boolean isAllDay;
 *     // ...
 * }
 * </pre>
 *
 * @see DateRangeValidator
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface ValidDateRange {

    /**
     * Error message when validation fails
     */
    String message() default "Start time must be before end time";

    /**
     * Validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Additional payload
     */
    Class<? extends Payload>[] payload() default {};
}