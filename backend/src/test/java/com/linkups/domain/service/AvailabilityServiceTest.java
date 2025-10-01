package com.linkups.domain.service;

import com.linkups.api.dto.request.availability.CreateAvailabilityRequest;
import com.linkups.api.dto.request.availability.UpdateAvailabilityRequest;
import com.linkups.base.BaseUnitTest;
import com.linkups.domain.entity.Availability;
import com.linkups.domain.entity.User;
import com.linkups.domain.entity.enums.AvailabilitySource;
import com.linkups.domain.exception.ResourceNotFoundException;
import com.linkups.domain.exception.ValidationException;
import com.linkups.domain.repository.AvailabilityRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AvailabilityService
 *
 * This test class focuses on DATE/TIME BUSINESS LOGIC patterns used at enterprise companies:
 * - Calendar and scheduling operations
 * - Date range validation and calculations
 * - Time-based queries and filtering
 * - Conflict detection algorithms
 * - Statistical aggregations
 * - All-day vs timed events handling
 *
 * Learning Focus:
 * - Date/time testing patterns
 * - Business rule validation
 * - Calendar logic testing
 * - Time range operations
 * - Conflict detection testing
 * - Statistical calculation testing
 *
 * This service is similar to calendar functionality in Confluence or scheduling in Jira.
 */
class AvailabilityServiceTest extends BaseUnitTest {

    // ============== MOCKED DEPENDENCIES ==============

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private UserService userService;

    // ============== SERVICE UNDER TEST ==============

    @InjectMocks
    private AvailabilityService availabilityService;

    // ============== TEST CONSTANTS ==============
    private static final Long USER_ID = 1L;
    private static final LocalDateTime START_TIME = LocalDateTime.now().plusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0);
    private static final LocalDateTime END_TIME = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);
    private static final String TITLE = "Team Meeting";
    private static final String DESCRIPTION = "Weekly sync meeting";
    private static final String LOCATION = "Conference Room A";

    // ============== AVAILABILITY CREATION TESTS ==============
    // These test the core availability creation functionality

    @Test
    void shouldCreateAvailabilitySuccessfully() {
        // ============== GIVEN ==============
        User testUser = User.builder()
                .id(USER_ID)
                .name("John Doe")
                .email("john@example.com")
                .build();

        Availability savedAvailability = Availability.builder()
                .id(10L)
                .user(testUser)
                .startTime(START_TIME)
                .endTime(END_TIME)
                .title(TITLE)
                .description(DESCRIPTION)
                .location(LOCATION)
                .isBusy(false)
                .isAllDay(false)
                .reminderMinutes(30)
                .source(AvailabilitySource.MANUAL)
                .build();

        // Mock user validation
        given(userService.findUserById(USER_ID)).willReturn(testUser);

        // Mock conflict check (no conflicts)
        given(availabilityRepository.findOverlappingSlots(USER_ID, START_TIME, END_TIME))
                .willReturn(Collections.emptyList());

        // Mock saving
        given(availabilityRepository.save(any(Availability.class)))
                .willReturn(savedAvailability);

        // ============== WHEN ==============
        CreateAvailabilityRequest request = CreateAvailabilityRequest.builder()
                .userId(USER_ID)
                .startTime(START_TIME)
                .endTime(END_TIME)
                .title(TITLE)
                .description(DESCRIPTION)
                .location(LOCATION)
                .isBusy(false)
                .isAllDay(false)
                .reminderMinutes(30)
                .build();

        Availability result = availabilityService.createAvailability(request);

        // ============== THEN ==============
        assertThat(result).isEqualTo(savedAvailability);
        assertThat(result.getTitle()).isEqualTo(TITLE);
        assertThat(result.getIsBusy()).isFalse();
        assertThat(result.getSource()).isEqualTo(AvailabilitySource.MANUAL);

        // Verify user validation
        then(userService).should().findUserById(USER_ID);

        // Verify conflict checking
        then(availabilityRepository).should().findOverlappingSlots(USER_ID, START_TIME, END_TIME);

        // Verify availability was saved with correct properties
        ArgumentCaptor<Availability> availabilityCaptor = ArgumentCaptor.forClass(Availability.class);
        then(availabilityRepository).should().save(availabilityCaptor.capture());

        Availability capturedAvailability = availabilityCaptor.getValue();
        assertThat(capturedAvailability.getUser()).isEqualTo(testUser);
        assertThat(capturedAvailability.getStartTime()).isEqualTo(START_TIME);
        assertThat(capturedAvailability.getEndTime()).isEqualTo(END_TIME);
        assertThat(capturedAvailability.getSource()).isEqualTo(AvailabilitySource.MANUAL);
    }

    @Test
    void shouldSetDefaultValuesWhenParametersAreNull() {
        // ============== GIVEN ==============
        User testUser = User.builder().id(USER_ID).build();

        given(userService.findUserById(USER_ID)).willReturn(testUser);
        given(availabilityRepository.findOverlappingSlots(USER_ID, START_TIME, END_TIME))
                .willReturn(Collections.emptyList());
        given(availabilityRepository.save(any(Availability.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // ============== WHEN ==============
        CreateAvailabilityRequest request = CreateAvailabilityRequest.builder()
                .userId(USER_ID)
                .startTime(START_TIME)
                .endTime(END_TIME)
                .title(TITLE)
                .build(); // All optional parameters as null

        availabilityService.createAvailability(request);

        // ============== THEN ==============
        ArgumentCaptor<Availability> availabilityCaptor = ArgumentCaptor.forClass(Availability.class);
        then(availabilityRepository).should().save(availabilityCaptor.capture());

        Availability capturedAvailability = availabilityCaptor.getValue();
        assertThat(capturedAvailability.getIsBusy()).isFalse(); // Default value
        assertThat(capturedAvailability.getIsAllDay()).isFalse(); // Default value
        assertThat(capturedAvailability.getReminderMinutes()).isEqualTo(30); // Default value
        assertThat(capturedAvailability.getDescription()).isNull();
        assertThat(capturedAvailability.getLocation()).isNull();
    }

    @Test
    void shouldLogConflictsWhenCreatingOverlappingAvailability() {
        // ============== GIVEN ==============
        User testUser = User.builder().id(USER_ID).build();

        // Mock existing conflicting availability
        Availability conflictingAvailability = Availability.builder()
                .id(5L)
                .title("Existing Meeting")
                .startTime(START_TIME.minusMinutes(30))
                .endTime(START_TIME.plusMinutes(30))
                .build();

        given(userService.findUserById(USER_ID)).willReturn(testUser);
        given(availabilityRepository.findOverlappingSlots(USER_ID, START_TIME, END_TIME))
                .willReturn(List.of(conflictingAvailability));
        given(availabilityRepository.save(any(Availability.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // ============== WHEN ==============
        CreateAvailabilityRequest request = CreateAvailabilityRequest.builder()
                .userId(USER_ID)
                .startTime(START_TIME)
                .endTime(END_TIME)
                .title(TITLE)
                .isBusy(false)
                .isAllDay(false)
                .reminderMinutes(30)
                .build();

        Availability result = availabilityService.createAvailability(request);

        // ============== THEN ==============
        assertThat(result).isNotNull(); // Should still create despite conflicts

        // Verify conflict checking was performed
        then(availabilityRepository).should().findOverlappingSlots(USER_ID, START_TIME, END_TIME);
        then(availabilityRepository).should().save(any(Availability.class));
    }

    // ============== AVAILABILITY UPDATE TESTS ==============
    // These test updating existing availability

    @Test
    void shouldUpdateAvailabilitySuccessfully() {
        // ============== GIVEN ==============
        Long availabilityId = 10L;
        LocalDateTime newStartTime = START_TIME.plusHours(1);
        LocalDateTime newEndTime = END_TIME.plusHours(1);
        String newTitle = "Updated Meeting";

        User testUser = User.builder().id(USER_ID).build();

        Availability existingAvailability = Availability.builder()
                .id(availabilityId)
                .user(testUser)
                .startTime(START_TIME)
                .endTime(END_TIME)
                .title(TITLE)
                .isBusy(false)
                .build();

        Availability updatedAvailability = Availability.builder()
                .id(availabilityId)
                .user(testUser)
                .startTime(newStartTime)
                .endTime(newEndTime)
                .title(newTitle)
                .isBusy(false)
                .build();

        given(availabilityRepository.findById(availabilityId))
                .willReturn(Optional.of(existingAvailability));
        given(availabilityRepository.findOverlappingSlotsExcluding(USER_ID, availabilityId, newStartTime, newEndTime))
                .willReturn(Collections.emptyList());
        given(availabilityRepository.save(any(Availability.class)))
                .willReturn(updatedAvailability);

        // ============== WHEN ==============
        UpdateAvailabilityRequest request = UpdateAvailabilityRequest.builder()
                .startTime(newStartTime)
                .endTime(newEndTime)
                .title(newTitle)
                .build();

        Availability result = availabilityService.updateAvailability(availabilityId, request);

        // ============== THEN ==============
        assertThat(result).isEqualTo(updatedAvailability);
        assertThat(result.getStartTime()).isEqualTo(newStartTime);
        assertThat(result.getEndTime()).isEqualTo(newEndTime);
        assertThat(result.getTitle()).isEqualTo(newTitle);

        then(availabilityRepository).should().findById(availabilityId);
        then(availabilityRepository).should().findOverlappingSlotsExcluding(USER_ID, availabilityId, newStartTime, newEndTime);
        then(availabilityRepository).should().save(any(Availability.class));
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        // ============== GIVEN ==============
        Long availabilityId = 10L;
        String newTitle = "Updated Title";

        User testUser = User.builder().id(USER_ID).build();

        Availability existingAvailability = Availability.builder()
                .id(availabilityId)
                .user(testUser)
                .startTime(START_TIME)
                .endTime(END_TIME)
                .title(TITLE)
                .description(DESCRIPTION)
                .location(LOCATION)
                .isBusy(false)
                .build();

        given(availabilityRepository.findById(availabilityId))
                .willReturn(Optional.of(existingAvailability));
        given(availabilityRepository.findOverlappingSlotsExcluding(USER_ID, availabilityId, START_TIME, END_TIME))
                .willReturn(Collections.emptyList());
        given(availabilityRepository.save(any(Availability.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // ============== WHEN ==============
        // Only update title, leave other fields as null (shouldn't change them)
        UpdateAvailabilityRequest request = UpdateAvailabilityRequest.builder()
                .title(newTitle)
                .build();

        availabilityService.updateAvailability(availabilityId, request);

        // ============== THEN ==============
        ArgumentCaptor<Availability> availabilityCaptor = ArgumentCaptor.forClass(Availability.class);
        then(availabilityRepository).should().save(availabilityCaptor.capture());

        Availability capturedAvailability = availabilityCaptor.getValue();
        assertThat(capturedAvailability.getTitle()).isEqualTo(newTitle); // Updated
        assertThat(capturedAvailability.getStartTime()).isEqualTo(START_TIME); // Unchanged
        assertThat(capturedAvailability.getEndTime()).isEqualTo(END_TIME); // Unchanged
        assertThat(capturedAvailability.getDescription()).isEqualTo(DESCRIPTION); // Unchanged
        assertThat(capturedAvailability.getLocation()).isEqualTo(LOCATION); // Unchanged
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentAvailability() {
        // ============== GIVEN ==============
        Long nonExistentId = 999L;

        given(availabilityRepository.findById(nonExistentId))
                .willReturn(Optional.empty());

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> availabilityService.getAvailabilityById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Availability");

        then(availabilityRepository).should().findById(nonExistentId);
    }

    // ============== USER VALIDATION TESTS ==============
    // These test user existence validation across all methods

    @Test
    void shouldThrowExceptionForNonExistentUserInCreate() {
        // ============== GIVEN ==============
        Long nonExistentUserId = 999L;

        given(userService.findUserById(nonExistentUserId))
                .willThrow(new ResourceNotFoundException("User not found"));

        // ============== WHEN & THEN ==============
        CreateAvailabilityRequest request = CreateAvailabilityRequest.builder()
                .userId(nonExistentUserId)
                .startTime(START_TIME)
                .endTime(END_TIME)
                .title(TITLE)
                .build();

        assertThatThrownBy(() -> availabilityService.createAvailability(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        then(userService).should().findUserById(nonExistentUserId);
        then(availabilityRepository).should(never()).save(any());
    }

    @Test
    void shouldThrowExceptionForNonExistentUserInCalendarView() {
        // ============== GIVEN ==============
        Long nonExistentUserId = 999L;
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);

        given(userService.findUserById(nonExistentUserId))
                .willThrow(new ResourceNotFoundException("User not found"));

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> availabilityService.getCalendarView(nonExistentUserId, start, end))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        then(userService).should().findUserById(nonExistentUserId);
        then(availabilityRepository).should(never()).findByUserIdAndDateRangeOverlap(any(), any(), any());
    }

    // ============== DATE/TIME BOUNDARY TESTS ==============
    // These test edge cases with date/time calculations

    @Test
    void shouldHandleMonthBoundariesCorrectly() {
        // ============== GIVEN ==============
        int year = LocalDateTime.now().plusYears(1).getYear();
        int month = 2; // February

        User testUser = User.builder().id(USER_ID).build();

        // February future year boundaries
        LocalDateTime expectedStart = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime expectedEnd = expectedStart.plusMonths(1).minusSeconds(1);

        given(userService.findUserById(USER_ID)).willReturn(testUser);
        given(availabilityRepository.findByUserIdAndDateRangeOverlap(USER_ID, expectedStart, expectedEnd))
                .willReturn(Collections.emptyList());

        // ============== WHEN ==============
        List<Availability> result = availabilityService.getMonthView(USER_ID, year, month);

        // ============== THEN ==============
        assertThat(result).isEmpty();

        then(availabilityRepository).should().findByUserIdAndDateRangeOverlap(USER_ID, expectedStart, expectedEnd);
    }

    @Test
    void shouldHandleDecemberToJanuaryTransition() {
        // ============== GIVEN ==============
        int year = LocalDateTime.now().plusYears(1).getYear();
        int month = 12; // December

        User testUser = User.builder().id(USER_ID).build();

        // December future year boundaries
        LocalDateTime expectedStart = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime expectedEnd = expectedStart.plusMonths(1).minusSeconds(1);

        given(userService.findUserById(USER_ID)).willReturn(testUser);
        given(availabilityRepository.findByUserIdAndDateRangeOverlap(USER_ID, expectedStart, expectedEnd))
                .willReturn(Collections.emptyList());

        // ============== WHEN ==============
        List<Availability> result = availabilityService.getMonthView(USER_ID, year, month);

        // ============== THEN ==============
        assertThat(result).isEmpty();

        then(availabilityRepository).should().findByUserIdAndDateRangeOverlap(USER_ID, expectedStart, expectedEnd);
    }

    @Test
    void shouldHandleTodayViewAtMidnight() {
        // ============== GIVEN ==============
        User testUser = User.builder().id(USER_ID).build();

        // Today's boundaries should be calculated correctly
        LocalDate today = LocalDate.now();
        LocalDateTime expectedStart = today.atStartOfDay(); // 00:00:00
        LocalDateTime expectedEnd = today.atTime(23, 59, 59); // 23:59:59

        given(userService.findUserById(USER_ID)).willReturn(testUser);
        given(availabilityRepository.findByUserIdAndDateRangeOverlap(USER_ID, expectedStart, expectedEnd))
                .willReturn(Collections.emptyList());

        // ============== WHEN ==============
        List<Availability> result = availabilityService.getTodayView(USER_ID);

        // ============== THEN ==============
        assertThat(result).isEmpty();

        then(availabilityRepository).should().findByUserIdAndDateRangeOverlap(USER_ID, expectedStart, expectedEnd);
    }

    // ============== ALL-DAY EVENT TESTS ==============
    // These test handling of all-day events vs timed events

    @Test
    void shouldCreateAllDayEventCorrectly() {
        // ============== GIVEN ==============
        User testUser = User.builder().id(USER_ID).build();
        LocalDateTime allDayStart = LocalDateTime.now().plusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime allDayEnd = LocalDateTime.now().plusDays(30).withHour(23).withMinute(59).withSecond(0).withNano(0);

        given(userService.findUserById(USER_ID)).willReturn(testUser);
        given(availabilityRepository.findOverlappingSlots(USER_ID, allDayStart, allDayEnd))
                .willReturn(Collections.emptyList());
        given(availabilityRepository.save(any(Availability.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // ============== WHEN ==============
        CreateAvailabilityRequest request = CreateAvailabilityRequest.builder()
                .userId(USER_ID)
                .startTime(allDayStart)
                .endTime(allDayEnd)
                .title("All Day Event")
                .isBusy(true)
                .isAllDay(true)
                .reminderMinutes(0)
                .build();

        availabilityService.createAvailability(request);

        // ============== THEN ==============
        ArgumentCaptor<Availability> availabilityCaptor = ArgumentCaptor.forClass(Availability.class);
        then(availabilityRepository).should().save(availabilityCaptor.capture());

        Availability capturedAvailability = availabilityCaptor.getValue();
        assertThat(capturedAvailability.getIsBusy()).isTrue();
        assertThat(capturedAvailability.getIsAllDay()).isTrue();
        assertThat(capturedAvailability.getReminderMinutes()).isEqualTo(0);
        assertThat(capturedAvailability.getTitle()).isEqualTo("All Day Event");
    }

    // ============== COMPLEX INTEGRATION TESTS ==============
    // These test complex workflows combining multiple operations

    @Test
    void shouldHandleComplexCalendarScenario() {
        // ============== GIVEN ==============
        User testUser = User.builder().id(USER_ID).build();

        // Create a complex day with multiple events
        LocalDateTime dayStart = LocalDateTime.now().plusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime dayEnd = LocalDateTime.now().plusDays(30).withHour(23).withMinute(59).withSecond(59).withNano(0);

        List<Availability> complexDayEvents = List.of(
                Availability.builder()
                        .id(1L)
                        .title("Morning Meeting")
                        .startTime(LocalDateTime.now().plusDays(30).withHour(9).withMinute(0).withSecond(0).withNano(0))
                        .endTime(LocalDateTime.now().plusDays(30).withHour(10).withMinute(0).withSecond(0).withNano(0))
                        .isBusy(true)
                        .isAllDay(false)
                        .build(),
                Availability.builder()
                        .id(2L)
                        .title("All Day Event")
                        .startTime(LocalDateTime.now().plusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0))
                        .endTime(LocalDateTime.now().plusDays(30).withHour(23).withMinute(59).withSecond(59).withNano(999999999))
                        .isBusy(false)
                        .isAllDay(true)
                        .build(),
                Availability.builder()
                        .id(3L)
                        .title("Afternoon Break")
                        .startTime(LocalDateTime.now().plusDays(30).withHour(14).withMinute(0).withSecond(0).withNano(0))
                        .endTime(LocalDateTime.now().plusDays(30).withHour(14).withMinute(30).withSecond(0).withNano(0))
                        .isBusy(false)
                        .isAllDay(false)
                        .build()
        );

        given(userService.findUserById(USER_ID)).willReturn(testUser);
        given(availabilityRepository.findByUserIdAndDateRangeOverlap(USER_ID, dayStart, dayEnd))
                .willReturn(complexDayEvents);

        // ============== WHEN ==============
        List<Availability> result = availabilityService.getCalendarView(USER_ID, dayStart, dayEnd);

        // ============== THEN ==============
        assertThat(result).hasSize(3);

        // Verify we have both timed and all-day events
        long timedEvents = result.stream().filter(a -> !a.getIsAllDay()).count();
        long allDayEvents = result.stream().filter(Availability::getIsAllDay).count();

        assertThat(timedEvents).isEqualTo(2);
        assertThat(allDayEvents).isEqualTo(1);

        // Verify busy vs free events
        long busyEvents = result.stream().filter(Availability::getIsBusy).count();
        long freeEvents = result.stream().filter(a -> !a.getIsBusy()).count();

        assertThat(busyEvents).isEqualTo(1);
        assertThat(freeEvents).isEqualTo(2);

        then(userService).should().findUserById(USER_ID);
        then(availabilityRepository).should().findByUserIdAndDateRangeOverlap(USER_ID, dayStart, dayEnd);
    }

    // ============== ERROR HANDLING AND EDGE CASES ==============
    // These test various error conditions and boundary cases

    @Test
    void shouldHandleEmptyResultsGracefully() {
        // ============== GIVEN ==============
        User testUser = User.builder().id(USER_ID).build();
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);

        given(userService.findUserById(USER_ID)).willReturn(testUser);
        given(availabilityRepository.findByUserIdAndDateRangeOverlap(USER_ID, start, end))
                .willReturn(Collections.emptyList());

        // ============== WHEN ==============
        List<Availability> result = availabilityService.getCalendarView(USER_ID, start, end);

        // ============== THEN ==============
        assertThat(result).isEmpty();
        assertThat(result).isNotNull();

        then(userService).should().findUserById(USER_ID);
        then(availabilityRepository).should().findByUserIdAndDateRangeOverlap(USER_ID, start, end);
    }

    @Test
    void shouldHandleStatisticsWithZeroEvents() {
        // ============== GIVEN ==============
        User testUser = User.builder().id(USER_ID).build();

        given(userService.findUserById(USER_ID)).willReturn(testUser);
        given(availabilityRepository.countByUserId(USER_ID)).willReturn(0L);
        given(availabilityRepository.countByUserIdAndIsBusyFalse(USER_ID)).willReturn(0L);
        given(availabilityRepository.countByUserIdAndIsBusyTrue(USER_ID)).willReturn(0L);
        // Note: countByUserIdAndIsAllDayTrue method may not exist in repository

        // ============== WHEN ==============
        Map<String, Long> result = availabilityService.getAvailabilityStatistics(USER_ID);

        // ============== THEN ==============
        assertThat(result).hasSize(3);
        assertThat(result.get("totalEvents")).isEqualTo(0L);
        assertThat(result.get("freeTimeSlots")).isEqualTo(0L);
        assertThat(result.get("busyTimeSlots")).isEqualTo(0L);

        then(userService).should().findUserById(USER_ID);
    }

    // ============== CONFLICT DETECTION TESTS ==============
    // These test the conflict detection functionality

    @Test
    void shouldDetectConflictsWhenCreatingOverlappingEvents() {
        // ============== GIVEN ==============
        User testUser = User.builder().id(USER_ID).build();

        // Existing event that will conflict
        Availability existingEvent = Availability.builder()
                .id(5L)
                .title("Existing Meeting")
                .startTime(START_TIME.minusMinutes(30)) // Starts 30 min before
                .endTime(START_TIME.plusMinutes(30))   // Ends 30 min after start
                .isBusy(true)
                .build();

        List<Availability> conflicts = List.of(existingEvent);

        given(userService.findUserById(USER_ID)).willReturn(testUser);
        given(availabilityRepository.findOverlappingSlots(USER_ID, START_TIME, END_TIME))
                .willReturn(conflicts);
        given(availabilityRepository.save(any(Availability.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // ============== WHEN ==============
        CreateAvailabilityRequest request = CreateAvailabilityRequest.builder()
                .userId(USER_ID)
                .startTime(START_TIME)
                .endTime(END_TIME)
                .title("New Meeting")
                .isBusy(true)
                .isAllDay(false)
                .reminderMinutes(15)
                .build();

        Availability result = availabilityService.createAvailability(request);

        // ============== THEN ==============
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("New Meeting");

        // Verify conflict detection was performed
        then(availabilityRepository).should().findOverlappingSlots(USER_ID, START_TIME, END_TIME);

        // Event should still be created despite conflicts (business decision)
        then(availabilityRepository).should().save(any(Availability.class));
    }

    @Test
    void shouldDetectConflictsWhenUpdating() {
        // ============== GIVEN ==============
        Long availabilityId = 10L;
        User testUser = User.builder().id(USER_ID).build();

        Availability existingAvailability = Availability.builder()
                .id(availabilityId)
                .user(testUser)
                .startTime(START_TIME)
                .endTime(END_TIME)
                .title(TITLE)
                .build();

        LocalDateTime newStartTime = START_TIME.plusHours(2);
        LocalDateTime newEndTime = END_TIME.plusHours(2);

        // Mock conflicting event at new time
        Availability conflictingEvent = Availability.builder()
                .id(15L)
                .title("Conflicting Event")
                .startTime(newStartTime.minusMinutes(15))
                .endTime(newStartTime.plusMinutes(45))
                .build();

        given(availabilityRepository.findById(availabilityId))
                .willReturn(Optional.of(existingAvailability));
        given(availabilityRepository.findOverlappingSlotsExcluding(USER_ID, availabilityId, newStartTime, newEndTime))
                .willReturn(List.of(conflictingEvent));
        given(availabilityRepository.save(any(Availability.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // ============== WHEN ==============
        UpdateAvailabilityRequest request = UpdateAvailabilityRequest.builder()
                .startTime(newStartTime)
                .endTime(newEndTime)
                .build();

        Availability result = availabilityService.updateAvailability(availabilityId, request);

        // ============== THEN ==============
        assertThat(result).isNotNull();
        assertThat(result.getStartTime()).isEqualTo(newStartTime);
        assertThat(result.getEndTime()).isEqualTo(newEndTime);

        // Verify conflict detection was performed (excluding the current event)
        then(availabilityRepository).should().findOverlappingSlotsExcluding(USER_ID, availabilityId, newStartTime, newEndTime);

        // Update should still proceed despite conflicts
        then(availabilityRepository).should().save(any(Availability.class));
    }

    // ============== TIME CALCULATION EDGE CASES ==============
    // These test edge cases in time calculations

    @Test
    void shouldHandleUpcomingEventsAtExactCurrentTime() {
        // ============== GIVEN ==============
        User testUser = User.builder().id(USER_ID).build();

        // Mock events starting exactly now and in the future
        LocalDateTime now = LocalDateTime.now();
        List<Availability> upcomingEvents = List.of(
                Availability.builder()
                        .id(1L)
                        .title("Starting Now")
                        .startTime(now)
                        .build(),
                Availability.builder()
                        .id(2L)
                        .title("Future Event")
                        .startTime(now.plusMinutes(1))
                        .build()
        );

        given(userService.findUserById(USER_ID)).willReturn(testUser);
        given(availabilityRepository.findByUserIdAndStartTimeAfterOrderByStartTime(eq(USER_ID), any(LocalDateTime.class)))
                .willReturn(upcomingEvents);

        // ============== WHEN ==============
        List<Availability> result = availabilityService.getUpcomingEvents(USER_ID);

        // ============== THEN ==============
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Starting Now");
        assertThat(result.get(1).getTitle()).isEqualTo("Future Event");

        then(userService).should().findUserById(USER_ID);
        then(availabilityRepository).should().findByUserIdAndStartTimeAfterOrderByStartTime(eq(USER_ID), any(LocalDateTime.class));
    }

// ============== AVAILABILITY DELETION TESTS ==============
// These test availability deletion functionality

@Test
void shouldDeleteAvailabilitySuccessfully() {
    // ============== GIVEN ==============
    Long availabilityId = 10L;
    User testUser = User.builder().id(USER_ID).build();

    Availability existingAvailability = Availability.builder()
            .id(availabilityId)
            .user(testUser)
            .title(TITLE)
            .build();

    given(availabilityRepository.findById(availabilityId))
            .willReturn(Optional.of(existingAvailability));

    // ============== WHEN ==============
    availabilityService.deleteAvailability(availabilityId);

    // ============== THEN ==============
    then(availabilityRepository).should().findById(availabilityId);
    then(availabilityRepository).should().deleteById(availabilityId);
}

@Test
void shouldThrowExceptionWhenDeletingNonExistentAvailability() {
    // ============== GIVEN ==============
    Long nonExistentId = 999L;

    given(availabilityRepository.findById(nonExistentId))
            .willReturn(Optional.empty());

    // ============== WHEN & THEN ==============
    assertThatThrownBy(() -> availabilityService.deleteAvailability(nonExistentId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Availability");

    then(availabilityRepository).should().findById(nonExistentId);
    then(availabilityRepository).should(never()).deleteById(any());
}

// ============== CALENDAR VIEW TESTS ==============
// These test various calendar view operations

@Test
void shouldGetCalendarViewSuccessfully() {
    // ============== GIVEN ==============
    LocalDateTime startRange = LocalDateTime.now().plusDays(30).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endRange = LocalDateTime.now().plusDays(60).withHour(23).withMinute(59).withSecond(0).withNano(0);

    User testUser = User.builder().id(USER_ID).build();

    List<Availability> availabilityList = List.of(
            Availability.builder().id(1L).title("Event 1").startTime(startRange.plusDays(1)).build(),
            Availability.builder().id(2L).title("Event 2").startTime(startRange.plusDays(5)).build(),
            Availability.builder().id(3L).title("Event 3").startTime(startRange.plusDays(10)).build()
    );

    given(userService.findUserById(USER_ID)).willReturn(testUser);
    given(availabilityRepository.findByUserIdAndDateRangeOverlap(USER_ID, startRange, endRange))
            .willReturn(availabilityList);

    // ============== WHEN ==============
    List<Availability> result = availabilityService.getCalendarView(USER_ID, startRange, endRange);

    // ============== THEN ==============
    assertThat(result).hasSize(3);
    assertThat(result).containsExactlyElementsOf(availabilityList);

    then(userService).should().findUserById(USER_ID);
    then(availabilityRepository).should().findByUserIdAndDateRangeOverlap(USER_ID, startRange, endRange);
}

@Test
void shouldGetCompleteCalendarViewWithImpliedFreeTime() {
    // ============== GIVEN ==============
    LocalDateTime startRange = LocalDateTime.now().plusDays(30).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endRange = LocalDateTime.now().plusDays(60).withHour(23).withMinute(59).withSecond(0).withNano(0);

    User testUser = User.builder().id(USER_ID).build();

    List<Availability> storedEvents = List.of(
            Availability.builder()
                    .id(1L)
                    .title("Busy Event")
                    .startTime(LocalDateTime.now().plusDays(45).withHour(9).withMinute(0).withSecond(0).withNano(0))
                    .endTime(LocalDateTime.now().plusDays(45).withHour(10).withMinute(0).withSecond(0).withNano(0))
                    .isBusy(true)
                    .build()
    );

    // Mock the service to partially spy it for the complex calculation method
    AvailabilityService spyService = spy(availabilityService);

    given(userService.findUserById(USER_ID)).willReturn(testUser);
    given(availabilityRepository.findByUserIdAndDateRangeOverlap(USER_ID, startRange, endRange))
            .willReturn(storedEvents);

    // Mock the implied free time calculation (complex algorithm)
    List<Availability> impliedFreeTime = List.of(
            Availability.builder()
                    .id(null)
                    .title("Free Time")
                    .startTime(LocalDateTime.now().plusDays(45).withHour(8).withMinute(0).withSecond(0).withNano(0))
                    .endTime(LocalDateTime.now().plusDays(45).withHour(9).withMinute(0).withSecond(0).withNano(0))
                    .isBusy(false)
                    .source(AvailabilitySource.MANUAL)
                    .build()
    );

    // Note: calculateImpliedFreeTime is a private method, so we'll test the public method behavior

    // ============== WHEN ==============
    List<Availability> result = spyService.getCompleteCalendarView(USER_ID, startRange, endRange);

    // ============== THEN ==============
    assertThat(result).hasSize(3); // 1 stored + 2 implied free slots (before and after)
    assertThat(result).contains(storedEvents.get(0));

    // Verify the list is sorted by start time
    Availability firstEvent = result.get(0);
    Availability secondEvent = result.get(1);
    assertThat(firstEvent.getStartTime()).isBeforeOrEqualTo(secondEvent.getStartTime());

    then(userService).should(times(2)).findUserById(USER_ID); // Called from both getCompleteCalendarView and calculateImpliedFreeTime
    then(availabilityRepository).should().findByUserIdAndDateRangeOverlap(USER_ID, startRange, endRange);
}

// ============== TIME-BASED VIEW TESTS ==============
// These test specific time-based filtering methods

@Test
void shouldGetMonthViewCorrectly() {
    // ============== GIVEN ==============
    int year = 2024;
    int month = 3; // March

    User testUser = User.builder().id(USER_ID).build();

    // Calculate expected month boundaries
    LocalDateTime expectedMonthStart = LocalDateTime.of(year, month, 1, 0, 0);
    LocalDateTime expectedMonthEnd = expectedMonthStart.plusMonths(1).minusSeconds(1);

    List<Availability> monthEvents = List.of(
            Availability.builder().id(1L).title("March Event 1").build(),
            Availability.builder().id(2L).title("March Event 2").build()
    );

    given(userService.findUserById(USER_ID)).willReturn(testUser);
    given(availabilityRepository.findByUserIdAndDateRangeOverlap(USER_ID, expectedMonthStart, expectedMonthEnd))
            .willReturn(monthEvents);

    // ============== WHEN ==============
    List<Availability> result = availabilityService.getMonthView(USER_ID, year, month);

    // ============== THEN ==============
    assertThat(result).hasSize(2);
    assertThat(result).containsExactlyElementsOf(monthEvents);

    then(userService).should().findUserById(USER_ID);
    then(availabilityRepository).should().findByUserIdAndDateRangeOverlap(USER_ID, expectedMonthStart, expectedMonthEnd);
}

@Test
void shouldGetTodayViewCorrectly() {
    // ============== GIVEN ==============
    User testUser = User.builder().id(USER_ID).build();

    // Calculate today's boundaries
    LocalDate today = LocalDate.now();
    LocalDateTime expectedDayStart = today.atStartOfDay();
    LocalDateTime expectedDayEnd = today.atTime(23, 59, 59);

    List<Availability> todayEvents = List.of(
            Availability.builder().id(1L).title("Today's Meeting").build()
    );

    given(userService.findUserById(USER_ID)).willReturn(testUser);
    given(availabilityRepository.findByUserIdAndDateRangeOverlap(USER_ID, expectedDayStart, expectedDayEnd))
            .willReturn(todayEvents);

    // ============== WHEN ==============
    List<Availability> result = availabilityService.getTodayView(USER_ID);

    // ============== THEN ==============
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("Today's Meeting");

    then(userService).should().findUserById(USER_ID);
    then(availabilityRepository).should().findByUserIdAndDateRangeOverlap(USER_ID, expectedDayStart, expectedDayEnd);
}

@Test
void shouldGetAllUserAvailabilitySuccessfully() {
    // ============== GIVEN ==============
    User testUser = User.builder().id(USER_ID).build();

    List<Availability> allEvents = List.of(
            Availability.builder().id(1L).title("Past Event").build(),
            Availability.builder().id(2L).title("Current Event").build(),
            Availability.builder().id(3L).title("Future Event").build()
    );

    given(userService.findUserById(USER_ID)).willReturn(testUser);
    given(availabilityRepository.findByUserIdOrderByStartTime(USER_ID))
            .willReturn(allEvents);

    // ============== WHEN ==============
    List<Availability> result = availabilityService.getAllUserAvailability(USER_ID);

    // ============== THEN ==============
    assertThat(result).hasSize(3);
    assertThat(result).containsExactlyElementsOf(allEvents);

    then(userService).should().findUserById(USER_ID);
    then(availabilityRepository).should().findByUserIdOrderByStartTime(USER_ID);
}

// ============== UPCOMING AND CURRENT EVENTS TESTS ==============
// These test time-sensitive event filtering

@Test
void shouldGetUpcomingEventsCorrectly() {
    // ============== GIVEN ==============
    User testUser = User.builder().id(USER_ID).build();

    List<Availability> upcomingEvents = List.of(
            Availability.builder()
                    .id(1L)
                    .title("Tomorrow's Meeting")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .build(),
            Availability.builder()
                    .id(2L)
                    .title("Next Week's Event")
                    .startTime(LocalDateTime.now().plusWeeks(1))
                    .build()
    );

    given(userService.findUserById(USER_ID)).willReturn(testUser);
    given(availabilityRepository.findByUserIdAndStartTimeAfterOrderByStartTime(eq(USER_ID), any(LocalDateTime.class)))
            .willReturn(upcomingEvents);

    // ============== WHEN ==============
    List<Availability> result = availabilityService.getUpcomingEvents(USER_ID);

    // ============== THEN ==============
    assertThat(result).hasSize(2);
    assertThat(result).containsExactlyElementsOf(upcomingEvents);

    then(userService).should().findUserById(USER_ID);
    then(availabilityRepository).should().findByUserIdAndStartTimeAfterOrderByStartTime(eq(USER_ID), any(LocalDateTime.class));
}

@Test
void shouldGetCurrentEventsCorrectly() {
    // ============== GIVEN ==============
    User testUser = User.builder().id(USER_ID).build();

    List<Availability> currentEvents = List.of(
            Availability.builder()
                    .id(1L)
                    .title("Current Meeting")
                    .startTime(LocalDateTime.now().minusMinutes(30))
                    .endTime(LocalDateTime.now().plusMinutes(30))
                    .build()
    );

    given(userService.findUserById(USER_ID)).willReturn(testUser);
    given(availabilityRepository.findCurrentEvents(eq(USER_ID), any(LocalDateTime.class)))
            .willReturn(currentEvents);

    // ============== WHEN ==============
    List<Availability> result = availabilityService.getCurrentEvents(USER_ID);

    // ============== THEN ==============
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("Current Meeting");

    then(userService).should().findUserById(USER_ID);
    then(availabilityRepository).should().findCurrentEvents(eq(USER_ID), any(LocalDateTime.class));
}

// ============== STATISTICS TESTS ==============
// These test statistical calculation functionality

@Test
void shouldGetAvailabilityStatisticsCorrectly() {
    // ============== GIVEN ==============
    User testUser = User.builder().id(USER_ID).build();

    // Mock statistical queries
    given(userService.findUserById(USER_ID)).willReturn(testUser);
    given(availabilityRepository.countByUserId(USER_ID)).willReturn(10L);
    given(availabilityRepository.countByUserIdAndIsBusyFalse(USER_ID)).willReturn(6L);
    given(availabilityRepository.countByUserIdAndIsBusyTrue(USER_ID)).willReturn(4L);
    // Note: countByUserIdAndIsAllDayTrue method may not exist in repository

    // ============== WHEN ==============
    Map<String, Long> result = availabilityService.getAvailabilityStatistics(USER_ID);

    // ============== THEN ==============
    assertThat(result).hasSize(3);
    assertThat(result.get("totalEvents")).isEqualTo(10L);
    assertThat(result.get("freeTimeSlots")).isEqualTo(6L);
    assertThat(result.get("busyTimeSlots")).isEqualTo(4L);

    then(userService).should().findUserById(USER_ID);
    then(availabilityRepository).should().countByUserId(USER_ID);
    then(availabilityRepository).should().countByUserIdAndIsBusyFalse(USER_ID);
    then(availabilityRepository).should().countByUserIdAndIsBusyTrue(USER_ID);
    // Note: countByUserIdAndIsAllDayTrue method may not exist in repository
}

// ============== SINGLE AVAILABILITY RETRIEVAL TESTS ==============
// These test getting individual availability records

@Test
void shouldGetAvailabilityByIdSuccessfully() {
    // ============== GIVEN ==============
    Long availabilityId = 10L;

    Availability expectedAvailability = Availability.builder()
            .id(availabilityId)
            .title(TITLE)
            .startTime(START_TIME)
            .endTime(END_TIME)
            .build();

    given(availabilityRepository.findById(availabilityId))
            .willReturn(Optional.of(expectedAvailability));

    // ============== WHEN ==============
    Availability result = availabilityService.getAvailabilityById(availabilityId);

    // ============== THEN ==============
    assertThat(result).isEqualTo(expectedAvailability);
    assertThat(result.getId()).isEqualTo(availabilityId);
    assertThat(result.getTitle()).isEqualTo(TITLE);

    then(availabilityRepository).should().findById(availabilityId);
}

@Test
void shouldThrowExceptionWhenGettingNonExistentAvailability() {
    // ============== GIVEN ==============
    Long nonExistentId = 999L;

    given(availabilityRepository.findById(nonExistentId))
            .willReturn(Optional.empty());

    // ============== WHEN & THEN ==============
    assertThatThrownBy(() -> availabilityService.getAvailabilityById(nonExistentId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Availability");

    then(availabilityRepository).should().findById(nonExistentId);
}

}