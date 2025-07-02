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

    List<ChatRoom> findByTyoe(ChatType type);
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

    

}