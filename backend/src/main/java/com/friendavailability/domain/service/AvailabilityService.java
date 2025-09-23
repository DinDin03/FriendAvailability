package com.friendavailability.domain.service;

import com.friendavailability.domain.entity.Availability;
import com.friendavailability.domain.entity.enums.AvailabilitySource;
import com.friendavailability.domain.entity.User;
import com.friendavailability.domain.exception.ResourceNotFoundException;
import com.friendavailability.domain.exception.ValidationException;
import com.friendavailability.domain.repository.AvailabilityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@Slf4j
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final UserService userService;

    public AvailabilityService(AvailabilityRepository availabilityRepository, UserService userService) {
        this.availabilityRepository = availabilityRepository;
        this.userService = userService;
        log.info("AvailabilityService created");
    }

    public Availability createAvailability(Long userId, LocalDateTime startTime, LocalDateTime endTime,
                                           String title, String description, String location,
                                           Boolean isBusy, Boolean isAllDay, Integer reminderMinutes) {

        log.debug("Creating availability for user {} with title: {}", userId, title);

        User user = userService.findUserById(userId);

        Availability availability = Availability.builder()
                .user(user)
                .startTime(startTime)
                .endTime(endTime)
                .title(title)
                .description(description)
                .location(location)
                .isBusy(isBusy != null ? isBusy : false)
                .isAllDay(isAllDay != null ? isAllDay : false)
                .reminderMinutes(reminderMinutes != null ? reminderMinutes : 30)
                .source(AvailabilitySource.MANUAL)
                .build();

        validateAvailability(availability);

        List<Availability> conflicts = checkConflicts(userId, startTime, endTime);
        if (!conflicts.isEmpty()) {
            log.warn("Found {} potential conflicts for new availability", conflicts.size());
            conflicts.forEach(c ->
                    log.debug("Conflict: {} ({} - {})", c.getTitle(), c.getStartTime(), c.getEndTime()));
        }

        Availability savedAvailability = availabilityRepository.save(availability);
        log.info("Created availability {} for user {}", savedAvailability.getId(), userId);
        return savedAvailability;
    }

    public Availability updateAvailability(Long id, LocalDateTime startTime, LocalDateTime endTime,
                                           String title, String description, String location,
                                           Boolean isBusy, Boolean isAllDay, Integer reminderMinutes) {

        log.debug("Updating availability with id {}", id);

        Availability availability = findAvailabilityById(id);

        if (startTime != null) availability.setStartTime(startTime);
        if (endTime != null) availability.setEndTime(endTime);
        if (title != null) availability.setTitle(title);
        if (description != null) availability.setDescription(description);
        if (location != null) availability.setLocation(location);
        if (isBusy != null) availability.setIsBusy(isBusy);
        if (isAllDay != null) availability.setIsAllDay(isAllDay);
        if (reminderMinutes != null) availability.setReminderMinutes(reminderMinutes);

        validateAvailability(availability);

        List<Availability> conflicts = availabilityRepository.findOverlappingSlotsExcluding(
                availability.getUser().getId(),
                id,
                availability.getStartTime(),
                availability.getEndTime()
        );

        if (!conflicts.isEmpty()) {
            log.warn("Found {} potential conflicts after update", conflicts.size());
        }

        Availability updatedAvailability = availabilityRepository.save(availability);
        log.info("Updated availability {} for user {}", id, availability.getUser().getId());
        return updatedAvailability;
    }

    public void deleteAvailability(Long id) {
        log.debug("Deleting availability with id {}", id);

        Availability availability = findAvailabilityById(id);
        availabilityRepository.deleteById(id);

        log.info("Deleted availability {} for user {}", id, availability.getUser().getId());
    }

    public Availability getAvailabilityById(Long id) {
        log.debug("Getting availability with id {}", id);

        return findAvailabilityById(id);
    }

    public List<Availability> getCalendarView(Long userId, LocalDateTime start, LocalDateTime end) {
        log.debug("Getting calendar view for user {} from {} to {}", userId, start, end);

        userService.findUserById(userId);

        List<Availability> availability = availabilityRepository.findByUserIdAndDateRangeOverlap(userId, start, end);
        log.debug("Found {} stored availability records for user {}", availability.size(), userId);

        return availability;
    }

    public List<Availability> getCompleteCalendarView(Long userId, LocalDateTime start, LocalDateTime end) {
        log.debug("Getting complete calendar view with implied free time for user {}", userId);

        userService.findUserById(userId);

        List<Availability> storedEvents = availabilityRepository.findByUserIdAndDateRangeOverlap(userId, start, end);
        log.debug("Found {} stored events for user {}", storedEvents.size(), userId);

        List<Availability> impliedFreeTime = calculateImpliedFreeTime(userId, start, end, storedEvents);
        log.debug("Calculated {} implied free time slots", impliedFreeTime.size());

        List<Availability> completeView = new ArrayList<>();
        completeView.addAll(storedEvents);
        completeView.addAll(impliedFreeTime);

        completeView.sort(Comparator.comparing(Availability::getStartTime));

        log.debug("Complete calendar view: {} total slots for user {}", completeView.size(), userId);
        return completeView;
    }

    public List<Availability> getMonthView(Long userId, int year, int month) {
        log.debug("Getting month view for user {} - {}/{}", userId, year, month);

        LocalDateTime monthStart = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
        return getCalendarView(userId, monthStart, monthEnd);
    }

    public List<Availability> getTodayView(Long userId) {
        log.debug("Getting today's availability for user {}", userId);

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(23, 59, 59);

        return getCalendarView(userId, dayStart, dayEnd);
    }

    public List<Availability> getAllUserAvailability(Long userId) {
        log.debug("Getting all availability for user {}", userId);

        userService.findUserById(userId);

        List<Availability> availability = availabilityRepository.findByUserIdOrderByStartTime(userId);
        log.debug("Found {} availability records for user {}", availability.size(), userId);

        return availability;
    }

    public List<Availability> getUpcomingEvents(Long userId) {
        log.debug("Getting upcoming events for user {}", userId);

        userService.findUserById(userId);

        LocalDateTime now = LocalDateTime.now();
        List<Availability> upcoming = availabilityRepository.findByUserIdAndStartTimeAfterOrderByStartTime(userId, now);

        log.debug("Found {} upcoming events for user {}", upcoming.size(), userId);
        return upcoming;
    }

    public List<Availability> getCurrentEvents(Long userId) {
        log.debug("Getting current events for user {}", userId);

        userService.findUserById(userId);

        LocalDateTime now = LocalDateTime.now();
        List<Availability> current = availabilityRepository.findCurrentEvents(userId, now);

        log.debug("Found {} current events for user {}", current.size(), userId);
        return current;
    }

    public Map<String, Long> getAvailabilityStatistics(Long userId) {
        log.debug("Getting availability statistics for user {}", userId);

        userService.findUserById(userId);

        Map<String, Long> stats = new HashMap<>();
        stats.put("totalEvents", availabilityRepository.countByUserId(userId));
        stats.put("freeTimeSlots", availabilityRepository.countByUserIdAndIsBusyFalse(userId));
        stats.put("busyTimeSlots", availabilityRepository.countByUserIdAndIsBusyTrue(userId));

        log.debug("Availability statistics for user {}: {}", userId, stats);
        return stats;
    }

    public boolean hasAvailabilityInRange(Long userId, LocalDateTime start, LocalDateTime end) {
        log.debug("Checking availability range for user {} from {} to {}", userId, start, end);

        userService.findUserById(userId);

        return availabilityRepository.existsByUserIdAndStartTimeBetween(userId, start, end);
    }

    private Availability findAvailabilityById(Long id) {
        return availabilityRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Availability not found with id {}", id);
                    return ResourceNotFoundException.availabilityNotFound(id);
                });
    }

    private List<Availability> calculateImpliedFreeTime(Long userId, LocalDateTime start, LocalDateTime end,
                                                        List<Availability> storedEvents) {
        log.debug("Calculating implied free time between {} events", storedEvents.size());

        User user = userService.findUserById(userId);
        List<Availability> freeSlots = new ArrayList<>();

        storedEvents.sort(Comparator.comparing(Availability::getStartTime));

        LocalDateTime currentTime = start;

        for (Availability event : storedEvents) {
            if (currentTime.isBefore(event.getStartTime())) {
                Availability freeSlot = createImpliedFreeSlot(user, currentTime, event.getStartTime());
                freeSlots.add(freeSlot);
                log.debug("Created free slot: {} to {}", currentTime, event.getStartTime());
            }
            if (event.getEndTime().isAfter(currentTime)) {
                currentTime = event.getEndTime();
            }
        }

        if (currentTime.isBefore(end)) {
            Availability freeSlot = createImpliedFreeSlot(user, currentTime, end);
            freeSlots.add(freeSlot);
            log.debug("Created final free slot: {} to {}", currentTime, end);
        }

        log.debug("Calculated {} implied free time slots", freeSlots.size());
        return freeSlots;
    }

    private Availability createImpliedFreeSlot(User user, LocalDateTime start, LocalDateTime end) {
        return Availability.builder()
                .user(user)
                .startTime(start)
                .endTime(end)
                .title("Available")
                .description("Implied free time")
                .isBusy(false)
                .isAllDay(false)
                .source(AvailabilitySource.MANUAL)
                .reminderMinutes(null)
                .build();
    }

    private List<Availability> checkConflicts(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Checking conflicts for user {} between {} and {}", userId, startTime, endTime);

        List<Availability> overlapping = availabilityRepository.findOverlappingSlots(userId, startTime, endTime);
        List<Availability> conflicts = overlapping.stream()
                .filter(Availability::getIsBusy)
                .toList();

        log.debug("Found {} potential conflicts for user {}", conflicts.size(), userId);
        return conflicts;
    }

    private void validateAvailability(Availability availability) {
        log.debug("Validating availability: {}", availability.getTitle());

        if (!availability.isValidTimeRange()) {
            log.warn("Invalid time range for availability: {} - {}",
                    availability.getStartTime(), availability.getEndTime());
            throw ValidationException.invalidTimeRange();
        }

        if (availability.getIsAllDay() && !availability.isValidAllDayEvent()) {
            log.warn("Invalid all day event configuration");
            throw ValidationException.invalidAllDayEvent();
        }

        if (availability.getStartTime().isBefore(LocalDateTime.now())) {
            log.warn("Attempted to create event in the past: {}", availability.getStartTime());
            throw ValidationException.eventInPast();
        }

        if (availability.getReminderMinutes() != null && availability.getReminderMinutes() < 0) {
            log.warn("Negative reminder minutes: {}", availability.getReminderMinutes());
            throw ValidationException.negativeReminder();
        }

        log.debug("Availability validation passed for: {}", availability.getTitle());
    }
}