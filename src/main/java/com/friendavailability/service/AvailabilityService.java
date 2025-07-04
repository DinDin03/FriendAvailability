package com.friendavailability.service;

import com.friendavailability.model.Availability;
import com.friendavailability.model.AvailabilitySource;
import com.friendavailability.model.User;
import com.friendavailability.repository.AvailabilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class AvailabilityService {
    private final AvailabilityRepository availabilityRepository;
    private final UserService userService;

    @Autowired
    public AvailabilityService(AvailabilityRepository availabilityRepository, UserService userService){
        this.availabilityRepository = availabilityRepository;
        this.userService = userService;
        System.out.println("AvailabilityService created");
    }

    public Availability createAvailability(Long userId, LocalDateTime startTime, LocalDateTime endTime,
                                           String title, String description, String location,
                                           Boolean isBusy, Boolean isAllDay, Integer reminderMinutes){

        System.out.println("Creating availability for user with id " + userId + ": " + description);

        User user = userService.findUserById(userId).
                orElseThrow(() -> new RuntimeException("User not found with id " + userId));

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
        if(!conflicts.isEmpty()){
            System.out.println("Warning: Found " + conflicts.size() + " potential conflicts for new availability");
            conflicts.forEach(c ->
                    System.out.println("Conflict: " + c.getTitle() + " (" + c.getStartTime() + "-" + c.getEndTime() + ")"));
        }
        Availability savedAvailability = availabilityRepository.save(availability);
        System.out.println("Created availability: " + savedAvailability);
        return savedAvailability;
    }

    public Optional<Availability> updateAvailability(Long id, LocalDateTime startTime, LocalDateTime endTime,
                                                     String title, String description, String location,
                                                     Boolean isBusy, Boolean isAllDay, Integer reminderMinutes){

        System.out.println("Updating availability with id " + id);
        Optional<Availability> availabilityOpt = availabilityRepository.findById(id);
        if(availabilityOpt.isEmpty()){
            System.out.println("Availability not found with id " + id);
            return Optional.empty();
        }

        Availability availability = availabilityOpt.get();

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
            System.out.println("Warning: Found " + conflicts.size() + " potential conflicts after update");
        }

        Availability updatedAvailability = availabilityRepository.save(availability);
        System.out.println("Updated availability: " + updatedAvailability);
        return Optional.of(updatedAvailability);
    }

    public boolean deleteAvailability(Long id){
        System.out.println("Deleting availability with id " + id);
        if(availabilityRepository.existsById(id)){
            availabilityRepository.deleteById(id);
            System.out.println("Deleted availability with id " + id);
            return true;
        }else{
            System.out.println("Availability not found with id: " + id);
            return false;
        }
    }

    public Optional<Availability> getAvailabilityById(Long id) {
        System.out.println("Getting availability with id: " + id);
        return availabilityRepository.findById(id);
    }

    public List<Availability> getCalendarView(Long userId, LocalDateTime start, LocalDateTime end) {
        System.out.println("Getting calendar view for user " + userId + " from " + start + " to " + end);

        List<Availability> availability = availabilityRepository.findByUserIdAndDateRangeOverlap(userId, start, end);
        System.out.println("Found " + availability.size() + " stored availability records");

        return availability;
    }

    public List<Availability> getCompleteCalendarView(Long userId, LocalDateTime start, LocalDateTime end) {
        System.out.println("Getting complete calendar view with implied free time for user " + userId);

        List<Availability> storedEvents = availabilityRepository.findByUserIdAndDateRangeOverlap(userId, start, end);
        System.out.println("Found " + storedEvents.size() + " stored events");

        List<Availability> impliedFreeTime = calculateImpliedFreeTime(userId, start, end, storedEvents);
        System.out.println("Calculated " + impliedFreeTime.size() + " implied free time slots");

        List<Availability> completeView = new ArrayList<>();
        completeView.addAll(storedEvents);
        completeView.addAll(impliedFreeTime);

        completeView.sort(Comparator.comparing(Availability::getStartTime));

        System.out.println("Complete calendar view: " + completeView.size() + " total slots");
        return completeView;
    }

    public List<Availability> getMonthView(Long userId, int year, int month){
        System.out.println("Getting month view for user " + userId + " - " + year + "/" + month);

        LocalDateTime monthStart = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
        return getCalendarView(userId, monthStart, monthEnd);
    }

    public List<Availability> getTodayView(Long userId) {
        System.out.println("Getting today's availability for user " + userId);

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(23, 59, 59);

        return getCalendarView(userId, dayStart, dayEnd);
    }

    public List<Availability> getAllUserAvailability(Long userId) {
        System.out.println("Getting all availability for user " + userId);

        List<Availability> availability = availabilityRepository.findByUserIdOrderByStartTime(userId);
        System.out.println("Found " + availability.size() + " availability records for user " + userId);

        return availability;
    }

    private List<Availability> calculateImpliedFreeTime(Long userId, LocalDateTime start, LocalDateTime end, List<Availability> storedEvents) {
        System.out.println("Calculating implied free time between " + storedEvents.size() + " events");

        User user = userService.findUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id " + userId));

        List<Availability> freeSlots = new ArrayList<>();

        storedEvents.sort(Comparator.comparing(Availability::getStartTime));

        LocalDateTime currentTime = start;

        for(Availability event : storedEvents){
            if(currentTime.isBefore(event.getStartTime())){
                Availability freeSlot = createImpliedFreeSlot(user, currentTime, event.getStartTime());
                freeSlots.add(freeSlot);
                System.out.println("Created free slot: " + currentTime + " to " + event.getStartTime());
            }
            if (event.getEndTime().isAfter(currentTime)) {
                currentTime = event.getEndTime();
            }
        }
        if(currentTime.isBefore(end)){
            Availability freeSlot = createImpliedFreeSlot(user, currentTime, end);
            freeSlots.add(freeSlot);
            System.out.println("Created final free slot: " + currentTime + " to " + end);
        }
        System.out.println("Calculated " + freeSlots.size() + " implied free time slots");
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
        System.out.println("Checking conflicts for user " + userId + " between " + startTime + " and " + endTime);
        List<Availability> overlapping = availabilityRepository.findOverlappingSlots(userId, startTime, endTime);
        List<Availability> conflicts = overlapping.stream()
                .filter(Availability::getIsBusy)
                .toList();
        System.out.println("Found " + conflicts.size() + " potential conflicts");
        return conflicts;
    }

    public void validateAvailability(Availability availability) {
        System.out.println("Validating availability: " + availability.getTitle());

        if (!availability.isValidTimeRange()) {
            throw new RuntimeException("Start time must be before end time");
        }

        if (availability.getIsAllDay() && !availability.isValidAllDayEvent()) {
            throw new RuntimeException("All day events must be full days");
        }

        if (availability.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot create events in the past");
        }

        if (availability.getReminderMinutes() != null && availability.getReminderMinutes() < 0) {
            throw new RuntimeException("Reminder cannot be negative");
        }

        System.out.println("Availability validation passed");
    }

    public List<Availability> getUpcomingEvents(Long userId) {
        System.out.println("Getting upcoming events for user " + userId);

        LocalDateTime now = LocalDateTime.now();
        List<Availability> upcoming = availabilityRepository.findByUserIdAndStartTimeAfterOrderByStartTime(userId, now);

        System.out.println("Found " + upcoming.size() + " upcoming events");
        return upcoming;
    }

    public List<Availability> getCurrentEvents(Long userId) {
        System.out.println("Getting current events for user " + userId);

        LocalDateTime now = LocalDateTime.now();
        List<Availability> current = availabilityRepository.findCurrentEvents(userId, now);

        System.out.println("Found " + current.size() + " current events");
        return current;
    }

    public Map<String, Long> getAvailabilityStatistics(Long userId) {
        System.out.println("Getting availability statistics for user " + userId);

        Map<String, Long> stats = new HashMap<>();
        stats.put("totalEvents", availabilityRepository.countByUserId(userId));
        stats.put("freeTimeSlots", availabilityRepository.countByUserIdAndIsBusyFalse(userId));
        stats.put("busyTimeSlots", availabilityRepository.countByUserIdAndIsBusyTrue(userId));

        System.out.println("Availability statistics: " + stats);
        return stats;
    }

    public boolean hasAvailabilityInRange(Long userId, LocalDateTime start, LocalDateTime end) {
        return availabilityRepository.existsByUserIdAndStartTimeBetween(userId, start, end);
    }

}
