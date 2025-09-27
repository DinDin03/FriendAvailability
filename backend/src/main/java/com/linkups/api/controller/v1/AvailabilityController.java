package com.linkups.api.controller.v1;

import com.linkups.api.dto.request.availability.CreateAvailabilityRequest;
import com.linkups.api.dto.request.availability.UpdateAvailabilityRequest;
import com.linkups.domain.entity.Availability;
import com.linkups.domain.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/availability")
@Slf4j
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
        log.info("AvailabilityController initialized successfully");
    }

    @PostMapping
    public ResponseEntity<Availability> createAvailability(@Valid @RequestBody CreateAvailabilityRequest request) {
        log.info("Creating availability for user {}", request.getUserId());

        Availability availability = availabilityService.createAvailability(
                request.getUserId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getTitle(),
                request.getDescription(),
                request.getLocation(),
                request.getIsBusy(),
                request.getIsAllDay(),
                request.getReminderMinutes()
        );

        log.info("Availability created successfully: {}", availability.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(availability);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Availability> updateAvailability(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateAvailabilityRequest request) {
        log.info("Updating availability {}", id);

        Availability updatedAvailability = availabilityService.updateAvailability(
                id,
                request.getStartTime(),
                request.getEndTime(),
                request.getTitle(),
                request.getDescription(),
                request.getLocation(),
                request.getIsBusy(),
                request.getIsAllDay(),
                request.getReminderMinutes()
        );

        log.info("Availability {} updated successfully", id);
        return ResponseEntity.ok(updatedAvailability);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteAvailability(@PathVariable Long id) {
        log.info("Deleting availability {}", id);

        availabilityService.deleteAvailability(id);

        Map<String, String> response = Map.of("message", "Availability deleted successfully");

        log.info("Availability {} deleted successfully", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/single/{id}")
    public ResponseEntity<Availability> getAvailabilityById(@PathVariable Long id) {
        log.info("Getting availability by ID: {}", id);

        Availability availability = availabilityService.getAvailabilityById(id);

        log.info("Retrieved availability: {}", id);
        return ResponseEntity.ok(availability);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Availability>> getUserAvailability(@PathVariable Long userId,
                                                                  @RequestParam LocalDateTime start,
                                                                  @RequestParam LocalDateTime end) {
        log.info("Getting availability for user {} from {} to {}", userId, start, end);

        List<Availability> availability = availabilityService.getCalendarView(userId, start, end);

        log.info("Retrieved {} availability records for user {}", availability.size(), userId);
        return ResponseEntity.ok(availability);
    }

    @GetMapping("/{userId}/complete")
    public ResponseEntity<List<Availability>> getCompleteCalendarView(@PathVariable Long userId,
                                                                      @RequestParam LocalDateTime start,
                                                                      @RequestParam LocalDateTime end) {
        log.info("Getting complete calendar view for user {} from {} to {}", userId, start, end);

        List<Availability> completeView = availabilityService.getCompleteCalendarView(userId, start, end);

        log.info("Retrieved complete calendar view with {} total slots for user {}", completeView.size(), userId);
        return ResponseEntity.ok(completeView);
    }

    @GetMapping("/{userId}/month")
    public ResponseEntity<List<Availability>> getMonthView(@PathVariable Long userId,
                                                           @RequestParam int year,
                                                           @RequestParam int month) {
        log.info("Getting month view for user {} - {}/{}", userId, year, month);

        List<Availability> monthView = availabilityService.getMonthView(userId, year, month);

        log.info("Retrieved month view with {} records for user {}", monthView.size(), userId);
        return ResponseEntity.ok(monthView);
    }

    @GetMapping("/{userId}/today")
    public ResponseEntity<List<Availability>> getTodayView(@PathVariable Long userId) {
        log.info("Getting today's availability for user {}", userId);

        List<Availability> todayView = availabilityService.getTodayView(userId);

        log.info("Retrieved today's view with {} records for user {}", todayView.size(), userId);
        return ResponseEntity.ok(todayView);
    }

    @GetMapping("/{userId}/all")
    public ResponseEntity<List<Availability>> getAllUserAvailability(@PathVariable Long userId) {
        log.info("Getting all availability for user {}", userId);

        List<Availability> allAvailability = availabilityService.getAllUserAvailability(userId);

        log.info("Retrieved {} total availability records for user {}", allAvailability.size(), userId);
        return ResponseEntity.ok(allAvailability);
    }

    @GetMapping("/{userId}/upcoming")
    public ResponseEntity<List<Availability>> getUpcomingEvents(@PathVariable Long userId) {
        log.info("Getting upcoming events for user {}", userId);

        List<Availability> upcoming = availabilityService.getUpcomingEvents(userId);

        log.info("Retrieved {} upcoming events for user {}", upcoming.size(), userId);
        return ResponseEntity.ok(upcoming);
    }

    @GetMapping("/{userId}/current")
    public ResponseEntity<List<Availability>> getCurrentEvents(@PathVariable Long userId) {
        log.info("Getting current events for user {}", userId);

        List<Availability> current = availabilityService.getCurrentEvents(userId);

        log.info("Retrieved {} current events for user {}", current.size(), userId);
        return ResponseEntity.ok(current);
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<Map<String, Long>> getAvailabilityStatistics(@PathVariable Long userId) {
        log.info("Getting availability statistics for user {}", userId);

        Map<String, Long> stats = availabilityService.getAvailabilityStatistics(userId);

        log.info("Retrieved availability statistics for user {}: {}", userId, stats);
        return ResponseEntity.ok(stats);
    }
}