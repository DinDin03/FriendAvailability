package com.friendavailability.repository;

import com.friendavailability.model.ChatParticipant;
import com.friendavailability.model.ParticipantRole;
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
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    List<ChatParticipant> findByChatRoomIdAndIsActiveTrue(Long chatRoomId);

    List<ChatParticipant> findByChatRoomId(Long chatRoomId);

    List<ChatParticipant> findByUserIdAndIsActiveTrue(Long userId);

    List<ChatParticipant> findByUserId(Long userId);

    @Query("SELECT COUNT(cp) > 0 FROM ChatParticipant cp " +
            "WHERE cp.userId = :userId " +
            "AND cp.chatRoomId = :chatRoomId " +
            "AND cp.isActive = true")
    boolean isUserActiveInRoom(@Param("userId") Long userId,
            @Param("chatRoomId") Long chatRoomId);

    @Query("SELECT cp FROM ChatParticipant cp " +
            "WHERE cp.userId = :userId " +
            "AND cp.chatRoomId = :chatRoomId " +
            "AND cp.isActive = true")
    Optional<ChatParticipant> findActiveParticipation(@Param("userId") Long userId,
            @Param("chatRoomId") Long chatRoomId);

    @Query("SELECT COUNT(cp) > 0 FROM ChatParticipant cp " +
            "WHERE cp.userId = :userId " +
            "AND cp.chatRoomId = :chatRoomId")
    boolean userExistsInRoom(@Param("userId") Long userId,
            @Param("chatRoomId") Long chatRoomId);

    @Query("SELECT COUNT(cp) > 0 FROM ChatParticipant cp " +
            "WHERE cp.userId = :userId " +
            "AND cp.chatRoomId = :chatRoomId " +
            "AND cp.role = 'ADMIN' " +
            "AND cp.isActive = true")
    boolean isUserAdminInRoom(@Param("userId") Long userId,
            @Param("chatRoomId") Long chatRoomId);

    @Query("SELECT cp FROM ChatParticipant cp " +
            "WHERE cp.chatRoomId = :chatRoomId " +
            "AND cp.role = 'ADMIN' " +
            "AND cp.isActive = true " +
            "ORDER BY cp.joinedAt ASC")
    List<ChatParticipant> findAdminsInRoom(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT cp FROM ChatParticipant cp " +
            "WHERE cp.chatRoomId = :chatRoomId " +
            "AND cp.role = 'MEMBER' " +
            "AND cp.isActive = true " +
            "ORDER BY cp.joinedAt ASC")
    List<ChatParticipant> findMembersInRoom(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT cp FROM ChatParticipant cp " +
            "WHERE cp.userId = :userId " +
            "AND cp.role = 'ADMIN' " +
            "AND cp.isActive = true " +
            "ORDER BY cp.joinedAt DESC")
    List<ChatParticipant> findRoomsWhereUserIsAdmin(@Param("userId") Long userId);

    @Query("SELECT cp FROM ChatParticipant cp " +
            "JOIN ChatRoom cr ON cp.chatRoomId = cr.id " +
            "WHERE cp.chatRoomId = :chatRoomId " +
            "AND cp.userId != :currentUserId " +
            "AND cp.isActive = true " +
            "AND cr.type = 'PRIVATE'")
    Optional<ChatParticipant> findOtherParticipantInPrivateChat(@Param("chatRoomId") Long chatRoomId,
            @Param("currentUserId") Long currentUserId);

    @Query("SELECT COUNT(cp) FROM ChatParticipant cp " +
            "JOIN ChatRoom cr ON cp.chatRoomId = cr.id " +
            "WHERE cp.chatRoomId = :chatRoomId " +
            "AND cp.isActive = true " +
            "AND cr.type = 'PRIVATE'")
    long countActiveParticipantsInPrivateChat(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT COUNT(cp) FROM ChatParticipant cp " +
            "WHERE cp.chatRoomId = :chatRoomId " +
            "AND cp.isActive = true")
    long countActiveParticipants(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT cp FROM ChatParticipant cp " +
            "WHERE cp.chatRoomId = :chatRoomId " +
            "AND cp.joinedAt > :afterDate " +
            "AND cp.isActive = true " +
            "ORDER BY cp.joinedAt DESC")
    List<ChatParticipant> findRecentParticipants(@Param("chatRoomId") Long chatRoomId,
            @Param("afterDate") LocalDateTime afterDate);

    @Query("SELECT cp FROM ChatParticipant cp " +
            "WHERE cp.chatRoomId = :chatRoomId " +
            "AND cp.isActive = true " +
            "ORDER BY cp.joinedAt ASC " +
            "LIMIT 1")
    Optional<ChatParticipant> findOldestParticipant(@Param("chatRoomId") Long chatRoomId);

    @Modifying
    @Transactional
    @Query("UPDATE ChatParticipant cp " +
            "SET cp.isActive = false " +
            "WHERE cp.userId = :userId " +
            "AND cp.chatRoomId = :chatRoomId")
    int removeUserFromRoom(@Param("userId") Long userId,
            @Param("chatRoomId") Long chatRoomId);

    @Modifying
    @Transactional
    @Query("UPDATE ChatParticipant cp " +
            "SET cp.isActive = true " +
            "WHERE cp.userId = :userId " +
            "AND cp.chatRoomId = :chatRoomId")
    int reactivateUserInRoom(@Param("userId") Long userId,
            @Param("chatRoomId") Long chatRoomId);

    @Modifying
    @Transactional
    @Query("UPDATE ChatParticipant cp " +
            "SET cp.role = :newRole " +
            "WHERE cp.userId = :userId " +
            "AND cp.chatRoomId = :chatRoomId " +
            "AND cp.isActive = true")
    int updateUserRole(@Param("userId") Long userId,
            @Param("chatRoomId") Long chatRoomId,
            @Param("newRole") ParticipantRole newRole);

    @Modifying
    @Transactional
    @Query("UPDATE ChatParticipant cp " +
            "SET cp.lastReadAt = :readTime " +
            "WHERE cp.userId = :userId " +
            "AND cp.chatRoomId = :chatRoomId " +
            "AND cp.isActive = true")
    int updateLastReadTime(@Param("userId") Long userId,
            @Param("chatRoomId") Long chatRoomId,
            @Param("readTime") LocalDateTime readTime);

    @Modifying
    @Transactional
    @Query("DELETE FROM ChatParticipant cp " +
            "WHERE cp.chatRoomId = :chatRoomId")
    int removeAllParticipantsFromRoom(@Param("chatRoomId") Long chatRoomId);

    @Modifying
    @Transactional
    @Query("UPDATE ChatParticipant cp " +
            "SET cp.isActive = false " +
            "WHERE cp.userId = :userId")
    int removeUserFromAllRooms(@Param("userId") Long userId);

    @Query("SELECT DISTINCT cp.chatRoomId FROM ChatParticipant cp " +
            "WHERE cp.chatRoomId NOT IN (" +
            "    SELECT cp2.chatRoomId FROM ChatParticipant cp2 " +
            "    WHERE cp2.isActive = true" +
            ")")
    List<Long> findEmptyChatRoomIds();

    @Query("SELECT COUNT(DISTINCT cp.chatRoomId) FROM ChatParticipant cp " +
            "WHERE cp.userId = :userId")
    long countTotalRoomsForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(cp) FROM ChatParticipant cp " +
            "WHERE cp.userId = :userId " +
            "AND cp.isActive = true")
    long countActiveRoomsForUser(@Param("userId") Long userId);

    @Query("SELECT cp.userId, COUNT(cp) as participationCount " +
            "FROM ChatParticipant cp " +
            "WHERE cp.isActive = true " +
            "GROUP BY cp.userId " +
            "ORDER BY participationCount DESC")
    List<Object[]> findMostActiveUsers();

    @Query("SELECT COUNT(cp), " +
            "       SUM(CASE WHEN cp.isActive = true THEN 1 ELSE 0 END), " +
            "       SUM(CASE WHEN cp.isActive = false THEN 1 ELSE 0 END) " +
            "FROM ChatParticipant cp " +
            "WHERE cp.joinedAt BETWEEN :startTime AND :endTime")
    Object[] getParticipationStatistics(@Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}