package com.friendavailability.controller;

import com.friendavailability.model.Availability;
import com.friendavailability.service.AvailabilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
        System.out.println("AvailabilityController created and connected to AvailabilityService");
    }

    @PostMapping
    public ResponseEntity<Availability> createAvailability(@Valid @RequestBody CreateAvailabilityRequest request) {
        System.out.println("Creating availability: " + request);

        try {
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

            return ResponseEntity.status(HttpStatus.CREATED).body(availability);

        } catch (RuntimeException e) {
            System.err.println("Business logic error creating availability: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error creating availability: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Availability> updateAvailability(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateAvailabilityRequest request) {
        System.out.println("Updating availability: " + request);

        try {
            Optional<Availability> result = availabilityService.updateAvailability(
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

            return result.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());

        } catch (RuntimeException e) {
            System.err.println("Business logic error updating availability: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error updating availability: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAvailability(@PathVariable Long id) {
        System.out.println("Deleting availability");

        try {
            boolean deleted = availabilityService.deleteAvailability(id);

            if (deleted) {
                return ResponseEntity.ok("Availability with id " + id + " deleted successfully");
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.err.println("Error deleting availability with id " + id + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/single/{id}")
    public ResponseEntity<Availability> getAvailabilityById(@PathVariable Long id) {
        System.out.println("Getting availability by ID");

        try {
            Optional<Availability> availability = availabilityService.getAvailabilityById(id);

            if (availability.isPresent()) {
                return ResponseEntity.ok(availability.get());
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.err.println("Error getting availability by id " + id + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Availability>> getUserAvailability(@PathVariable Long userId,
                                                                  @RequestParam LocalDateTime start,
                                                                  @RequestParam LocalDateTime end) {
        System.out.println("Getting stored availability from " + start + " to " + end);

        try {
            List<Availability> availability = availabilityService.getCalendarView(userId, start, end);
            return ResponseEntity.ok(availability);

        } catch (RuntimeException e) {
            System.err.println("Business logic error getting availability: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error getting availability for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{userId}/complete")
    public ResponseEntity<List<Availability>> getCompleteCalendarView(@PathVariable Long userId,
                                                                      @RequestParam LocalDateTime start,
                                                                      @RequestParam LocalDateTime end) {
        System.out.println("Getting COMPLETE calendar view (with free time) from " + start + " to " + end);

        try {
            List<Availability> completeView = availabilityService.getCompleteCalendarView(userId, start, end);
            System.out.println("Returning " + completeView.size() + " total slots (stored events + implied free time)");
            return ResponseEntity.ok(completeView);

        } catch (RuntimeException e) {
            System.err.println("Business logic error getting complete calendar view: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error getting complete calendar view for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{userId}/month")
    public ResponseEntity<List<Availability>> getMonthView(@PathVariable Long userId,
                                                           @RequestParam int year,
                                                           @RequestParam int month) {
        System.out.println("Getting month view for " + year + "/" + month);

        try {
            List<Availability> monthView = availabilityService.getMonthView(userId, year, month);
            return ResponseEntity.ok(monthView);

        } catch (RuntimeException e) {
            System.err.println("Business logic error getting month view: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error getting month view for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{userId}/today")
    public ResponseEntity<List<Availability>> getTodayView(@PathVariable Long userId) {
        System.out.println("Getting today's availability");

        try {
            List<Availability> todayView = availabilityService.getTodayView(userId);
            return ResponseEntity.ok(todayView);

        } catch (RuntimeException e) {
            System.err.println("Business logic error getting today view: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error getting today view for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{userId}/all")
    public ResponseEntity<List<Availability>> getAllUserAvailability(@PathVariable Long userId) {
        System.out.println("Getting all availability for user");

        try {
            List<Availability> allAvailability = availabilityService.getAllUserAvailability(userId);
            return ResponseEntity.ok(allAvailability);

        } catch (RuntimeException e) {
            System.err.println("Business logic error getting all availability: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error getting all availability for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{userId}/upcoming")
    public ResponseEntity<List<Availability>> getUpcomingEvents(@PathVariable Long userId) {
        System.out.println("Getting upcoming events");

        try {
            List<Availability> upcoming = availabilityService.getUpcomingEvents(userId);
            return ResponseEntity.ok(upcoming);

        } catch (Exception e) {
            System.err.println("Error getting upcoming events for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{userId}/current")
    public ResponseEntity<List<Availability>> getCurrentEvents(@PathVariable Long userId) {
        System.out.println("Getting current events");

        try {
            List<Availability> current = availabilityService.getCurrentEvents(userId);
            return ResponseEntity.ok(current);

        } catch (Exception e) {
            System.err.println("Error getting current events for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<Map<String, Long>> getAvailabilityStatistics(@PathVariable Long userId) {
        System.out.println("Getting availability statistics");

        try {
            Map<String, Long> stats = availabilityService.getAvailabilityStatistics(userId);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            System.err.println("Error getting availability statistics for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Setter
    @Getter
    public static class CreateAvailabilityRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotNull(message = "Start time is required")
        private LocalDateTime startTime;

        @NotNull(message = "End time is required")
        private LocalDateTime endTime;

        private String title;
        private String description;
        private String location;
        private Boolean isBusy;
        private Boolean isAllDay;
        private Integer reminderMinutes;

        public CreateAvailabilityRequest() {}

        public CreateAvailabilityRequest(Long userId, LocalDateTime startTime, LocalDateTime endTime, String title) {
            this.userId = userId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.title = title;
        }

        @Override
        public String toString() {
            return "CreateAvailabilityRequest{" +
                    "userId=" + userId +
                    ", startTime=" + startTime +
                    ", endTime=" + endTime +
                    ", title='" + title + '\'' +
                    ", isBusy=" + isBusy +
                    ", isAllDay=" + isAllDay +
                    '}';
        }
    }

    @Setter
    @Getter
    public static class UpdateAvailabilityRequest {
        // Getters and Setters
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String title;
        private String description;
        private String location;
        private Boolean isBusy;
        private Boolean isAllDay;
        private Integer reminderMinutes;

        public UpdateAvailabilityRequest() {}

        @Override
        public String toString() {
            return "UpdateAvailabilityRequest{" +
                    "startTime=" + startTime +
                    ", endTime=" + endTime +
                    ", title='" + title + '\'' +
                    ", isBusy=" + isBusy +
                    ", isAllDay=" + isAllDay +
                    '}';
        }
    }
}