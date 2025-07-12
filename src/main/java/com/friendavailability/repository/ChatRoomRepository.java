package com.friendavailability.repository;

import com.friendavailability.model.ChatRoom;
import com.friendavailability.model.ChatType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByType(ChatType type);
    List<ChatRoom> findByCreatedByOrderByCreatedAtDesc(Long createdBy);
    List<ChatRoom> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date);

    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
           "JOIN cr.participants cp " +
           "WHERE cp.userId = :userId " +
           "AND cp.isActive = true " +
           "ORDER BY cr.updatedAt DESC")
    List<ChatRoom> findChatRoomsForUser(@Param("userId") Long userId);

    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
            "JOIN cr.participants cp " +
            "WHERE cp.userId = :userId " +
            "AND cp.isActive = true " +
            "ORDER BY cr.updatedAt DESC")
    Page<ChatRoom> findChatRoomsForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT cr) FROM ChatRoom cr " +
           "JOIN cr.participants cp " +
           "WHERE cp.userId = :userId " +
           "AND cp.isActive = true")
    long countChatRoomsForUser(@Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr " +
    "WHERE cr.type = 'PRIVATE' " +
    "AND cr.id IN (" +
    "    SELECT cp1.chatRoomId FROM ChatParticipant cp1 " +
    "    WHERE cp1.userId = :userId1 AND cp1.isActive = true" +
    ") " +
    "AND cr.id IN (" +
    "    SELECT cp2.chatRoomId FROM ChatParticipant cp2 " +
    "    WHERE cp2.userId = :userId2 AND cp2.isActive = true" +
    ")")
    Optional<ChatRoom> findPrivateChatBetweenUsers(@Param("userId1") Long userId1,
    @Param("userId2") Long userId2);

    @Query("SELECT COUNT(cr) > 0 FROM ChatRoom cr " +
           "WHERE cr.type = 'PRIVATE' " +
           "AND cr.id IN (" +
           "    SELECT cp1.chatRoomId FROM ChatParticipant cp1 " +
           "    WHERE cp1.userId = :userId1 AND cp1.isActive = true" +
           ") " +
           "AND cr.id IN (" +
           "    SELECT cp2.chatRoomId FROM ChatParticipant cp2 " +
           "    WHERE cp2.userId = :userId2 AND cp2.isActive = true" +
           ")")
    boolean existsPrivateChatBetweenUsers(@Param("userId1") Long userId1, 
    @Param("userId2") Long userId2);

    @Query("SELECT cr FROM ChatRoom cr " +
           "WHERE cr.type = 'GROUP' " +
           "AND LOWER(cr.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "ORDER BY cr.name")
    List<ChatRoom> findGroupChatsByNameContaining(@Param("name") String name);

    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
           "JOIN cr.participants cp " +
           "WHERE cp.userId = :userId " +
           "AND cp.isActive = true " +
           "AND cp.role = 'ADMIN' " +
           "ORDER BY cr.updatedAt DESC")
    List<ChatRoom> findChatRoomsWhereUserIsAdmin(@Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr " +
           "WHERE cr.id NOT IN (" +
           "    SELECT DISTINCT cp.chatRoomId FROM ChatParticipant cp " +
           "    WHERE cp.isActive = true" +
           ")")
    List<ChatRoom> findEmptyChatRooms();

    @Query("SELECT cr FROM ChatRoom cr " +
           "WHERE cr.updatedAt >= :since " +
           "ORDER BY cr.updatedAt DESC")
    List<ChatRoom> findRecentlyActiveChatRooms(@Param("since") LocalDateTime since);

}