package com.linkups.domain.repository;

import com.linkups.domain.entity.UserStatus;
import com.linkups.domain.entity.enums.UserStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

    Optional<UserStatus> findByUserId(Long userId);
    List<UserStatus> findByStatus(UserStatusType status);
    List<UserStatus> findByStatusIn(List<UserStatusType> statuses);
    List<UserStatus> findByUserIdIn(List<Long> userIds);

    // Find all users who are currently online (not offline)
    @Query("SELECT us FROM UserStatus us WHERE us.status != :offlineStatus")
    List<UserStatus> findAllOnlineUsers(@Param("offlineStatus") UserStatusType offlineStatus);

    // Update user status efficiently without loading the entity
    @Modifying
    @Query("UPDATE UserStatus us SET us.status = :status, us.lastSeen = :lastSeen, us.updatedAt = :updatedAt WHERE us.userId = :userId")
    int updateUserStatus(@Param("userId") Long userId,
                         @Param("status") UserStatusType status,
                         @Param("lastSeen") LocalDateTime lastSeen,
                         @Param("updatedAt") LocalDateTime updatedAt);

    // Count users by status type (useful for statistics)
    @Query("SELECT COUNT(us) FROM UserStatus us WHERE us.status = :status")
    long countByStatus(@Param("status") UserStatusType status);

    // Find users who haven't been seen for a certain period
    @Query("SELECT us FROM UserStatus us WHERE us.lastSeen < :cutoffTime")
    List<UserStatus> findUsersNotSeenSince(@Param("cutoffTime") LocalDateTime cutoffTime);

}