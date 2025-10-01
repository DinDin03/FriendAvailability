package com.linkups.domain.repository;

import com.linkups.domain.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Activity Repository - Data access layer for Activity entities
 *
 * This repository provides methods for querying activities with various filters
 * and supports the activity feed functionality with pagination and filtering.
 */
@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    /**
     * Find activities for a user's activity feed
     * Gets activities from the user's friends ordered by timestamp
     *
     * @param friendIds List of friend user IDs
     * @param isActive Whether to include only active activities
     * @param visibility Visibility levels to include
     * @param pageable Pagination parameters
     * @return Page of activities
     */
    @Query("SELECT a FROM Activity a WHERE a.userId IN :friendIds AND a.isActive = :isActive AND a.visibility IN :visibility ORDER BY a.timestamp DESC")
    Page<Activity> findActivitiesByFriends(
        @Param("friendIds") List<Long> friendIds,
        @Param("isActive") Boolean isActive,
        @Param("visibility") List<String> visibility,
        Pageable pageable
    );

    /**
     * Find activities for a user's activity feed with type filtering
     *
     * @param friendIds List of friend user IDs
     * @param activityTypes List of activity types to include
     * @param isActive Whether to include only active activities
     * @param visibility Visibility levels to include
     * @param pageable Pagination parameters
     * @return Page of activities
     */
    @Query("SELECT a FROM Activity a WHERE a.userId IN :friendIds AND a.type IN :activityTypes AND a.isActive = :isActive AND a.visibility IN :visibility ORDER BY a.timestamp DESC")
    Page<Activity> findActivitiesByFriendsAndTypes(
        @Param("friendIds") List<Long> friendIds,
        @Param("activityTypes") List<String> activityTypes,
        @Param("isActive") Boolean isActive,
        @Param("visibility") List<String> visibility,
        Pageable pageable
    );

    /**
     * Find activities for a user's activity feed with date range filtering
     *
     * @param friendIds List of friend user IDs
     * @param startDate Start date for filtering
     * @param endDate End date for filtering
     * @param isActive Whether to include only active activities
     * @param visibility Visibility levels to include
     * @param pageable Pagination parameters
     * @return Page of activities
     */
    @Query("SELECT a FROM Activity a WHERE a.userId IN :friendIds AND a.timestamp BETWEEN :startDate AND :endDate AND a.isActive = :isActive AND a.visibility IN :visibility ORDER BY a.timestamp DESC")
    Page<Activity> findActivitiesByFriendsAndDateRange(
        @Param("friendIds") List<Long> friendIds,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("isActive") Boolean isActive,
        @Param("visibility") List<String> visibility,
        Pageable pageable
    );

    /**
     * Find activities for a user's activity feed with both type and date filtering
     *
     * @param friendIds List of friend user IDs
     * @param activityTypes List of activity types to include
     * @param startDate Start date for filtering
     * @param endDate End date for filtering
     * @param isActive Whether to include only active activities
     * @param visibility Visibility levels to include
     * @param pageable Pagination parameters
     * @return Page of activities
     */
    @Query("SELECT a FROM Activity a WHERE a.userId IN :friendIds AND a.type IN :activityTypes AND a.timestamp BETWEEN :startDate AND :endDate AND a.isActive = :isActive AND a.visibility IN :visibility ORDER BY a.timestamp DESC")
    Page<Activity> findActivitiesByFriendsTypesAndDateRange(
        @Param("friendIds") List<Long> friendIds,
        @Param("activityTypes") List<String> activityTypes,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("isActive") Boolean isActive,
        @Param("visibility") List<String> visibility,
        Pageable pageable
    );

    /**
     * Find activities by a specific user
     *
     * @param userId User ID
     * @param isActive Whether to include only active activities
     * @param pageable Pagination parameters
     * @return Page of activities
     */
    Page<Activity> findByUserIdAndIsActiveOrderByTimestampDesc(
        Long userId,
        Boolean isActive,
        Pageable pageable
    );

    /**
     * Find activities by type and user
     *
     * @param userId User ID
     * @param type Activity type
     * @param isActive Whether to include only active activities
     * @param pageable Pagination parameters
     * @return Page of activities
     */
    Page<Activity> findByUserIdAndTypeAndIsActiveOrderByTimestampDesc(
        Long userId,
        String type,
        Boolean isActive,
        Pageable pageable
    );

    /**
     * Count activities by user in a date range
     *
     * @param userId User ID
     * @param startDate Start date
     * @param endDate End date
     * @param isActive Whether to include only active activities
     * @return Count of activities
     */
    @Query("SELECT COUNT(a) FROM Activity a WHERE a.userId = :userId AND a.timestamp BETWEEN :startDate AND :endDate AND a.isActive = :isActive")
    Long countActivitiesByUserAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("isActive") Boolean isActive
    );

    /**
     * Find recent activities by friends for a specific user
     *
     * @param friendIds List of friend user IDs
     * @param limit Maximum number of activities to return
     * @param isActive Whether to include only active activities
     * @param visibility Visibility levels to include
     * @return List of recent activities
     */
    @Query("SELECT a FROM Activity a WHERE a.userId IN :friendIds AND a.isActive = :isActive AND a.visibility IN :visibility ORDER BY a.timestamp DESC LIMIT :limit")
    List<Activity> findRecentActivitiesByFriends(
        @Param("friendIds") List<Long> friendIds,
        @Param("limit") int limit,
        @Param("isActive") Boolean isActive,
        @Param("visibility") List<String> visibility
    );

    /**
     * Find activities by related entity
     *
     * @param relatedEntityId Related entity ID
     * @param relatedEntityType Related entity type
     * @param isActive Whether to include only active activities
     * @param pageable Pagination parameters
     * @return Page of activities
     */
    Page<Activity> findByRelatedEntityIdAndRelatedEntityTypeAndIsActiveOrderByTimestampDesc(
        Long relatedEntityId,
        String relatedEntityType,
        Boolean isActive,
        Pageable pageable
    );

    /**
     * Delete old activities (for cleanup)
     *
     * @param cutoffDate Activities older than this date will be deleted
     * @return Number of deleted activities
     */
    @Query("DELETE FROM Activity a WHERE a.timestamp < :cutoffDate")
    Long deleteActivitiesOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find activities with high priority
     *
     * @param friendIds List of friend user IDs
     * @param minPriority Minimum priority level
     * @param isActive Whether to include only active activities
     * @param visibility Visibility levels to include
     * @param pageable Pagination parameters
     * @return Page of high priority activities
     */
    @Query("SELECT a FROM Activity a WHERE a.userId IN :friendIds AND a.priority >= :minPriority AND a.isActive = :isActive AND a.visibility IN :visibility ORDER BY a.priority DESC, a.timestamp DESC")
    Page<Activity> findHighPriorityActivitiesByFriends(
        @Param("friendIds") List<Long> friendIds,
        @Param("minPriority") Integer minPriority,
        @Param("isActive") Boolean isActive,
        @Param("visibility") List<String> visibility,
        Pageable pageable
    );
}