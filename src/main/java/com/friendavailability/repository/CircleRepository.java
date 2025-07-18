package com.friendavailability.repository;

import com.friendavailability.model.Circle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CircleRepository extends JpaRepository<Circle, Long> {

    @Query("SELECT c FROM Circle c WHERE c.createdBy = :userId AND c.isActive = true ORDER BY c.createdAt DESC")
    List<Circle> findCirclesCreatedByUser(@Param("userId") Long userId);

    List<Circle> findByCreatedByOrderByCreatedAtDesc(Long createdBy);

    @Query("SELECT c FROM Circle c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND c.isActive = true")
    List<Circle> findActiveCirclesByNameContaining(@Param("searchTerm") String searchTerm);

    @Query("SELECT DISTINCT c FROM Circle c " +
            "JOIN c.members cm " +
            "WHERE cm.userId = :userId " +
            "AND cm.isActive = true " +
            "AND c.isActive = true " +
            "ORDER BY c.updatedAt DESC")
    List<Circle> findCirclesForUser(@Param("userId") Long userId);

    @Query("SELECT DISTINCT c FROM Circle c " +
            "JOIN c.members cm " +
            "WHERE cm.userId = :userId " +
            "AND cm.isActive = true " +
            "AND c.isActive = true " +
            "AND (cm.role = 'OWNER' OR cm.role = 'ADMIN') " +
            "ORDER BY c.updatedAt DESC")
    List<Circle> findCirclesUserCanManage(@Param("userId") Long userId);

    @Query("SELECT COUNT(cm) > 0 FROM Circle c " +
            "JOIN c.members cm " +
            "WHERE c.id = :circleId " +
            "AND cm.userId = :userId " +
            "AND cm.isActive = true " +
            "AND c.isActive = true")
    boolean isUserMemberOfCircle(@Param("userId") Long userId, @Param("circleId") Long circleId);

    @Query("SELECT COUNT(cm) > 0 FROM Circle c " +
            "JOIN c.members cm " +
            "WHERE c.id = :circleId " +
            "AND cm.userId = :userId " +
            "AND cm.isActive = true " +
            "AND c.isActive = true " +
            "AND (cm.role = 'OWNER' OR cm.role = 'ADMIN')")
    boolean canUserManageCircle(@Param("userId") Long userId, @Param("circleId") Long circleId);

    @Query("SELECT COUNT(c) FROM Circle c WHERE c.createdBy = :userId AND c.isActive = true")
    long countActiveCirclesCreatedByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT c) FROM Circle c " +
            "JOIN c.members cm " +
            "WHERE cm.userId = :userId " +
            "AND cm.isActive = true " +
            "AND c.isActive = true")
    long countCirclesForUser(@Param("userId") Long userId);

    @Query("SELECT c FROM Circle c " +
            "WHERE c.isActive = true " +
            "AND c.isPrivate = false " +
            "ORDER BY SIZE(c.members) DESC")
    List<Circle> findPopularPublicCircles();

    @Query("SELECT c FROM Circle c WHERE c.createdAt BETWEEN :startDate AND :endDate ORDER BY c.createdAt DESC")
    List<Circle> findCirclesCreatedBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c FROM Circle c WHERE c.updatedAt < :cutoffDate AND c.isActive = true")
    List<Circle> findStaleCircles(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Transactional
    @Query("UPDATE Circle c SET c.isActive = false, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :circleId")
    int softDeleteCircle(@Param("circleId") Long circleId);

    @Modifying
    @Transactional
    @Query("UPDATE Circle c SET c.isActive = true, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :circleId")
    int reactivateCircle(@Param("circleId") Long circleId);

    List<Circle> findByIsActiveTrueOrderByCreatedAtDesc();

    List<Circle> findByIsPrivateAndIsActiveTrueOrderByCreatedAtDesc(Boolean isPrivate);

    @Query("SELECT COUNT(c) > 0 FROM Circle c WHERE c.createdBy = :userId AND LOWER(c.name) = LOWER(:name) AND c.isActive = true")
    boolean existsActiveCircleWithNameForUser(@Param("userId") Long userId, @Param("name") String name);

    Optional<Circle> findByNameAndCreatedByAndIsActiveTrue(String name, Long createdBy);
}