package com.friendavailability.repository;

import com.friendavailability.model.Message;
import com.friendavailability.model.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatRoomIdOrderBySentAtDesc(Long chatRoomId);

    Page<Message> findByChatRoomIdOrderBySentAtDesc(Long chatRoomId, Pageable pageable);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoomId = :chatRoomId " +
            "ORDER BY m.sentAt DESC " +
            "LIMIT :limit")
    List<Message> findRecentMessagesInRoom(@Param("chatRoomId") Long chatRoomId,
            @Param("limit") int limit);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoomId = :chatRoomId " +
            "AND m.sentAt > :afterTime " +
            "ORDER BY m.sentAt ASC")
    List<Message> findMessagesAfterTime(@Param("chatRoomId") Long chatRoomId,
            @Param("afterTime") LocalDateTime afterTime);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoomId = :chatRoomId " +
            "AND m.sentAt BETWEEN :startTime AND :endTime " +
            "ORDER BY m.sentAt ASC")
    List<Message> findMessagesBetweenTimes(@Param("chatRoomId") Long chatRoomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoomId = :chatRoomId " +
            "AND DATE(m.sentAt) = CURRENT_DATE " +
            "ORDER BY m.sentAt ASC")
    List<Message> findTodaysMessages(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.chatRoomId = :chatRoomId " +
            "AND m.sentAt > :lastReadTime " +
            "AND m.senderId != :userId")
    long countUnreadMessages(@Param("chatRoomId") Long chatRoomId,
            @Param("userId") Long userId,
            @Param("lastReadTime") LocalDateTime lastReadTime);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoomId = :chatRoomId " +
            "AND m.sentAt > :lastReadTime " +
            "AND m.senderId != :userId " +
            "ORDER BY m.sentAt ASC")
    List<Message> findUnreadMessages(@Param("chatRoomId") Long chatRoomId,
            @Param("userId") Long userId,
            @Param("lastReadTime") LocalDateTime lastReadTime);

    @Query("SELECT COUNT(m) FROM Message m " +
            "JOIN ChatParticipant cp ON m.chatRoomId = cp.chatRoomId " +
            "WHERE cp.userId = :userId " +
            "AND cp.isActive = true " +
            "AND m.sentAt > COALESCE(cp.lastReadAt, '1970-01-01') " +
            "AND m.senderId != :userId")
    long countTotalUnreadMessagesForUser(@Param("userId") Long userId);

    List<Message> findByChatRoomIdAndMessageTypeOrderBySentAtDesc(Long chatRoomId, MessageType messageType);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoomId = :chatRoomId " +
            "AND m.messageType = 'TEXT' " +
            "ORDER BY m.sentAt DESC")
    List<Message> findTextMessagesInRoom(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoomId = :chatRoomId " +
            "AND m.messageType = 'SYSTEM_MESSAGE' " +
            "ORDER BY m.sentAt DESC")
    List<Message> findSystemMessagesInRoom(@Param("chatRoomId") Long chatRoomId);

    List<Message> findBySenderIdOrderBySentAtDesc(Long senderId);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoomId = :chatRoomId " +
            "AND m.senderId = :senderId " +
            "ORDER BY m.sentAt DESC")
    List<Message> findUserMessagesInRoom(@Param("chatRoomId") Long chatRoomId,
            @Param("senderId") Long senderId);

    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.senderId = :senderId " +
            "AND m.sentAt BETWEEN :startTime AND :endTime")
    long countUserMessagesInPeriod(@Param("senderId") Long senderId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoomId = :chatRoomId " +
            "AND m.messageType = 'TEXT' " +
            "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "ORDER BY m.sentAt DESC")
    List<Message> searchMessagesInRoom(@Param("chatRoomId") Long chatRoomId,
            @Param("searchTerm") String searchTerm);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoomId = :chatRoomId " +
            "ORDER BY m.sentAt DESC " +
            "LIMIT 1")
    Message findLastMessageInRoom(@Param("chatRoomId") Long chatRoomId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Message m " +
            "WHERE m.sentAt < :cutoffDate")
    int deleteMessagesOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Transactional
    @Query("DELETE FROM Message m " +
            "WHERE m.chatRoomId = :chatRoomId")
    int deleteAllMessagesInRoom(@Param("chatRoomId") Long chatRoomId);

    long countByChatRoomId(Long chatRoomId);

    @Query("SELECT DISTINCT m.chatRoomId FROM Message m")
    List<Long> findChatRoomIdsWithMessages();

    @Query("SELECT m.chatRoomId, COUNT(m) as messageCount " +
            "FROM Message m " +
            "WHERE m.sentAt >= :since " +
            "GROUP BY m.chatRoomId " +
            "ORDER BY messageCount DESC")
    List<Object[]> findMostActiveRoomsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(m), COUNT(DISTINCT m.senderId) " +
            "FROM Message m " +
            "WHERE m.sentAt BETWEEN :startTime AND :endTime")
    Object[] getMessageStatistics(@Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

}
