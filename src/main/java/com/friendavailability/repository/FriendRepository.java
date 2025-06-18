package com.friendavailability.repository;

import com.friendavailability.model.Friend;
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
public interface FriendRepository extends JpaRepository<Friend, Long> {

    @Query("SELECT f FROM Friend f WHERE f.userId = :userId OR f.friendId = :userId")
    List<Friend> findAllFriendshipsForUser(@Param("userId") Long userId);

    @Query("SELECT f FROM Friend f WHERE " +
            "(f.userId = :userId1 AND f.friendId = :userId2) OR " +
            "(f.userId = :userId2 AND f.friendId = :userId1)")
    Optional<Friend> findFriendshipBetweenUsers(@Param("userId1") Long userId1,
                                                @Param("userId2") Long userId2);

    @Query("SELECT COUNT(f) > 0 FROM Friend f WHERE " +
            "(f.userId = :userId1 AND f.friendId = :userId2) OR " +
            "(f.userId = :userId2 AND f.friendId = :userId1)")
    boolean existsFriendshipBetweenUsers(@Param("userId1") Long userId1,
                                         @Param("userId2") Long userId2);

    List<Friend> findByFriendIdAndStatus(Long friendId, String status);

    List<Friend> findByUserIdAndStatus(Long userId, String status);

    @Query("SELECT f FROM Friend f WHERE " +
            "(f.userId = :userId OR f.friendId = :userId) AND f.status = 'ACCEPTED'")
    List<Friend> findAcceptedFriendshipsForUser(@Param("userId") Long userId);

    @Query("SELECT f FROM Friend f WHERE " +
            "(f.userId = :userId OR f.friendId = :userId) AND f.status = 'PENDING'")
    List<Friend> findPendingFriendshipsForUser(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Friend f WHERE f.userId = :userId OR f.friendId = :userId")
    void deleteAllFriendshipsForUser(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Friend f WHERE " +
            "(f.userId = :userId1 AND f.friendId = :userId2) OR " +
            "(f.userId = :userId2 AND f.friendId = :userId1)")
    void deleteFriendshipBetweenUsers(@Param("userId1") Long userId1,
                                      @Param("userId2") Long userId2);

    @Query("SELECT COUNT(f) FROM Friend f WHERE " +
            "(f.userId = :userId OR f.friendId = :userId) AND f.status = 'ACCEPTED'")
    long countFriendsForUser(@Param("userId") Long userId);

    long countByFriendIdAndStatus(Long friendId, String status);

    List<Friend> findByStatusAndCreatedAtAfterOrderByCreatedAtDesc(String status, LocalDateTime date);

}