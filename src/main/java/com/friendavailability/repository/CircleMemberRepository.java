package com.friendavailability.repository;

import com.friendavailability.model.CircleMember;
import com.friendavailability.model.CircleRole;
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
public interface CircleMemberRepository extends JpaRepository<CircleMember, Long> {

    @Query("SELECT COUNT(cm) > 0 FROM CircleMember cm " +
           "WHERE cm.userId = :userId AND cm.circleId = :circleId " +
           "AND cm.isActive = true")
    boolean isUserActiveMemberOfCircle(@Param("userId") Long userId, @Param("circleId") Long circleId);

    @Query("SELECT COUNT(cm) > 0 FROM CircleMember cm " +
           "WHERE cm.userId = :userId AND cm.circleId = :circleId")
    boolean userExistsInCircle(@Param("userId") Long userId, @Param("circleId") Long circleId);

    @Query("SELECT COUNT(cm) > 0 FROM CircleMember cm " +
           "WHERE cm.userId = :userId AND cm.circleId = :circleId " +
           "AND cm.isActive = true " +
           "AND (cm.role = 'ADMIN' OR cm.role = 'OWNER')")
    boolean isUserAdminOrOwnerOfCircle(@Param("userId") Long userId, @Param("circleId") Long circleId);

    @Query("SELECT COUNT(cm) > 0 FROM CircleMember cm " +
           "WHERE cm.userId = :userId AND cm.circleId = :circleId " +
           "AND cm.isActive = true AND cm.role = 'OWNER'")
    boolean isUserOwnerOfCircle(@Param("userId") Long userId, @Param("circleId") Long circleId);

    @Query("SELECT cm FROM CircleMember cm " +
           "WHERE cm.circleId = :circleId AND cm.isActive = true " +
           "ORDER BY cm.role ASC, cm.joinedAt ASC")
    List<CircleMember> findActiveCircleMembers(@Param("circleId") Long circleId);

    @Query("SELECT cm FROM CircleMember cm " +
           "WHERE cm.circleId = :circleId AND cm.isActive = true AND cm.role = :role " +
           "ORDER BY cm.joinedAt ASC")
    List<CircleMember> findActiveMembersByRole(@Param("circleId") Long circleId, @Param("role") CircleRole role);

    @Query("SELECT cm FROM CircleMember cm " +
           "WHERE cm.userId = :userId AND cm.isActive = true " +
           "ORDER BY cm.joinedAt DESC")
    List<CircleMember> findActiveCircleMembershipsForUser(@Param("userId") Long userId);

    @Query("SELECT cm FROM CircleMember cm " +
           "WHERE cm.userId = :userId AND cm.isActive = true " +
           "AND (cm.role = 'ADMIN' OR cm.role = 'OWNER') " +
           "ORDER BY cm.joinedAt DESC")
    List<CircleMember> findCirclesUserCanManage(@Param("userId") Long userId);

    @Query("SELECT cm FROM CircleMember cm " +
           "WHERE cm.userId = :userId AND cm.circleId = :circleId")
    Optional<CircleMember> findMembershipRecord(@Param("userId") Long userId, @Param("circleId") Long circleId);

    @Query("SELECT cm FROM CircleMember cm " +
           "WHERE cm.userId = :userId AND cm.circleId = :circleId AND cm.isActive = true")
    Optional<CircleMember> findActiveMembershipRecord(@Param("userId") Long userId, @Param("circleId") Long circleId);

    @Query("SELECT cm FROM CircleMember cm " +
           "WHERE cm.circleId = :circleId AND cm.role = 'OWNER' AND cm.isActive = true")
    Optional<CircleMember> findCircleOwner(@Param("circleId") Long circleId);

    @Query("SELECT COUNT(cm) FROM CircleMember cm " +
           "WHERE cm.circleId = :circleId AND cm.isActive = true")
    long countActiveMembersInCircle(@Param("circleId") Long circleId);

    @Query("SELECT COUNT(cm) FROM CircleMember cm " +
           "WHERE cm.userId = :userId AND cm.isActive = true")
    long countActiveCircleMembershipsForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(cm) FROM CircleMember cm " +
           "WHERE cm.circleId = :circleId AND cm.isActive = true AND cm.role = :role")
    long countMembersByRole(@Param("circleId") Long circleId, @Param("role") CircleRole role);

    @Modifying
    @Transactional
    @Query("UPDATE CircleMember cm SET cm.isActive = false, cm.leftAt = CURRENT_TIMESTAMP, " +
           "cm.lastActivityAt = CURRENT_TIMESTAMP " +
           "WHERE cm.userId = :userId AND cm.circleId = :circleId")
    int deactivateMembership(@Param("userId") Long userId, @Param("circleId") Long circleId);

    @Modifying
    @Transactional
    @Query("UPDATE CircleMember cm SET cm.isActive = true, cm.leftAt = null, " +
           "cm.lastActivityAt = CURRENT_TIMESTAMP " +
           "WHERE cm.userId = :userId AND cm.circleId = :circleId")
    int reactivateMembership(@Param("userId") Long userId, @Param("circleId") Long circleId);

    @Modifying
    @Transactional
    @Query("UPDATE CircleMember cm SET cm.role = :newRole, cm.lastActivityAt = CURRENT_TIMESTAMP " +
           "WHERE cm.id = :membershipId AND cm.isActive = true")
    int updateMemberRole(@Param("membershipId") Long membershipId, @Param("newRole") CircleRole newRole);

    @Modifying
    @Transactional
    @Query("UPDATE CircleMember cm SET cm.lastActivityAt = CURRENT_TIMESTAMP " +
           "WHERE cm.userId = :userId AND cm.circleId = :circleId AND cm.isActive = true")
    int updateLastActivity(@Param("userId") Long userId, @Param("circleId") Long circleId);

    @Modifying
    @Transactional
    @Query("UPDATE CircleMember cm SET cm.isActive = false, cm.leftAt = CURRENT_TIMESTAMP " +
           "WHERE cm.circleId = :circleId")
    int deactivateAllMembershipsForCircle(@Param("circleId") Long circleId);

    @Modifying
    @Transactional
    @Query("UPDATE CircleMember cm SET cm.isActive = false, cm.leftAt = CURRENT_TIMESTAMP " +
           "WHERE cm.userId = :userId")
    int deactivateAllMembershipsForUser(@Param("userId") Long userId);

    @Query("SELECT cm FROM CircleMember cm " +
           "WHERE cm.isActive = false AND cm.leftAt < :cutoffDate")
    List<CircleMember> findOldInactiveMemberships(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT cm FROM CircleMember cm " +
           "WHERE cm.circleId = :circleId AND cm.isActive = true " +
           "AND cm.joinedAt > :sinceDate " +
           "ORDER BY cm.joinedAt DESC")
    List<CircleMember> findRecentMembers(@Param("circleId") Long circleId, @Param("sinceDate") LocalDateTime sinceDate);

    @Query("SELECT cm FROM CircleMember cm " +
           "WHERE cm.circleId = :circleId AND cm.isActive = true " +
           "AND (cm.lastActivityAt IS NULL OR cm.lastActivityAt < :cutoffDate) " +
           "ORDER BY cm.lastActivityAt ASC NULLS FIRST")
    List<CircleMember> findInactiveMembers(@Param("circleId") Long circleId, @Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT cm1.circleId FROM CircleMember cm1 " +
           "JOIN CircleMember cm2 ON cm1.circleId = cm2.circleId " +
           "WHERE cm1.userId = :userId1 AND cm2.userId = :userId2 " +
           "AND cm1.isActive = true AND cm2.isActive = true")
    List<Long> findCommonCircles(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    List<CircleMember> findByCircleIdAndIsActiveTrueOrderByJoinedAtAsc(Long circleId);

    List<CircleMember> findByUserIdAndIsActiveTrueOrderByJoinedAtDesc(Long userId);

    Optional<CircleMember> findByUserIdAndCircleId(Long userId, Long circleId);

    long countByCircleIdAndIsActiveTrue(Long circleId);

    long countByUserIdAndIsActiveTrue(Long userId);
}