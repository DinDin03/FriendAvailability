package com.friendavailability.repository;

import com.friendavailability.model.Availability;
import com.friendavailability.model.AvailabilitySource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long>{
    List<Availability> findByUserId(Long userId);
    List<Availability> findByUserIdOrderByStartTime(Long userId);
    List<Availability> findByUserIdAndSource(Long userId, AvailabilitySource availabilitySource);
    List<Availability> findByUserIdAndStartTimeBetweenOrderByStartTime(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM Availability a WHERE a.user.id = :userId AND a.startTime <= :endTime AND a.endTime >= :startTime ORDER BY a.startTime")
    List<Availability> findByUserIdAndDateRangeOverlap(@Param("userId") Long userId,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    List<Availability> findByUserIdAndIsBusyFalseOrderByStartTime(Long userId);
    List<Availability> findByUserIdAndIsBusyTrueOrderByStartTime(Long userId);
    List<Availability> findByUserIdAndIsBusyFalseAndStartTimeBetweenOrderByStartTime(
            Long userId, LocalDateTime start, LocalDateTime end);
    List<Availability> findByUserIdAndIsBusyTrueAndStartTimeBetweenOrderByStartTime(
            Long userId, LocalDateTime start, LocalDateTime end);
    List<Availability> findByUserIdAndIsAllDayTrueOrderByStartTime(Long userId);
    List<Availability> findByUserIdAndIsAllDayFalseOrderByStartTime(Long userId);
    List<Availability> findByUserIdAndIsAllDayTrueAndStartTimeBetweenOrderByStartTime(
            Long userId, LocalDateTime start, LocalDateTime end);
    List<Availability> findByUserIdAndIsAllDayFalseAndStartTimeBetweenOrderByStartTime(
            Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM Availability a WHERE a.user.id = :userId AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Availability> findOverlappingSlots(@Param("userId") Long userId,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT a FROM Availability a WHERE a.user.id = :userId AND a.id != :excludeId AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Availability> findOverlappingSlotsExcluding(@Param("userId") Long userId,
                                                     @Param("excludeId") Long excludeId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(a) > 0 FROM Availability a WHERE a.user.id = :userId AND a.startTime < :endTime AND a.endTime > :startTime")
    boolean hasOverlappingSlots(@Param("userId") Long userId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    long countByUserId(Long userId);
    long countByUserIdAndStartTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);
    long countByUserIdAndIsBusyFalse(Long userId);
    long countByUserIdAndIsBusyTrue(Long userId);
    boolean existsByUserId(Long userId);
    boolean existsByUserIdAndStartTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM Availability a WHERE a.user.id = :userId AND a.startTime >= :dayStart AND a.startTime < :dayEnd ORDER BY a.startTime")
    List<Availability> findByUserIdAndDate(@Param("userId") Long userId,
                                           @Param("dayStart") LocalDateTime dayStart,
                                           @Param("dayEnd") LocalDateTime dayEnd);

    List<Availability> findByUserIdAndStartTimeAfterOrderByStartTime(Long userId, LocalDateTime now);
    List<Availability> findByUserIdAndEndTimeBeforeOrderByStartTimeDesc(Long userId, LocalDateTime now);

    @Query("SELECT a FROM Availability a WHERE a.user.id = :userId AND a.startTime <= :now AND a.endTime >= :now ORDER BY a.startTime")
    List<Availability> findCurrentEvents(@Param("userId") Long userId, @Param("now") LocalDateTime now);

//    @Query("SELECT a FROM Availability a WHERE a.user.id = :userId AND a.reminderMinutes IS NOT NULL AND (a.startTime - INTERVAL a.reminderMinutes MINUTE) <= :now AND a.startTime > :now")
//    List<Availability> findEventsWithDueReminders(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    List<Availability> findByUserIdAndLocationContainingIgnoreCaseOrderByStartTime(Long userId, String location);
    List<Availability> findByUserIdAndTitleContainingIgnoreCaseOrderByStartTime(Long userId, String title);
    Optional<Availability> findFirstByUserIdAndStartTimeAfterOrderByStartTime(Long userId, LocalDateTime now);
    Optional<Availability> findFirstByUserIdAndEndTimeBeforeOrderByEndTimeDesc(Long userId, LocalDateTime now);

    List<Availability> findByUserIdAndEndTimeBefore(Long userId, LocalDateTime cutoffDate);
    void deleteByUserIdAndEndTimeBefore(Long userId, LocalDateTime cutoffDate);
    List<Availability> findByUserIdAndReminderMinutesIsNullOrderByStartTime(Long userId);
    List<Availability> findByUserIdAndReminderMinutesOrderByStartTime(Long userId, Integer reminderMinutes);
}
