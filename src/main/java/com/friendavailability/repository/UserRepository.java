
package com.friendavailability.repository;

import com.friendavailability.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{

    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByEmail(String email);
    boolean existsByGoogleId(String googleId);

    List<User> findByIsActiveTrue();
    List<User> findByEmailVerifiedTrue();
    List<User> findByIsActiveTrueAndEmailVerifiedTrue();
    List<User> findByNameContainingIgnoreCase(String name);
    List<User> findByCreatedAtAfter(LocalDateTime date);
    long countByIsActiveTrue();
    long countByEmailVerifiedTrue();

    @Query("SELECT u FROM User u WHERE u.email LIKE CONCAT('%@', :domain)")
    List<User> findByEmailDomain(@Param("domain") String domain);

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchByNameOrEmail(@Param("searchTerm") String searchTerm);

    @Query("SELECT " +
            "COUNT(u), " +
            "SUM(CASE WHEN u.isActive = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN u.emailVerified = true THEN 1 ELSE 0 END) " +
            "FROM User u")
    Object[] getUserStatistics();

    @Query("SELECT u FROM User u WHERE u.createdAt >= :cutoffDate AND u.isActive = true ORDER BY u.createdAt DESC")
    List<User> findRecentlyActiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);
}
