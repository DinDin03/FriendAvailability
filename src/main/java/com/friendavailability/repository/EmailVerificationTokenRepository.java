package com.friendavailability.repository;

import com.friendavailability.model.EmailVerificationToken;
import com.friendavailability.model.User;
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
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long>{

    Optional<EmailVerificationToken> findByToken(String token);

    List<EmailVerificationToken> findByUserOrderByCreatedAtDesc(User user);

    Optional<EmailVerificationToken> findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(User user, LocalDateTime now);

    boolean existsByUserAndUsedFalseAndExpiresAtAfter(User user, LocalDateTime now);

    List<EmailVerificationToken> findByExpiresAtBefore(LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerificationToken evt WHERE evt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerificationToken evt WHERE evt.user = :user")
    int deleteAllByUser(@Param("user") User user);

    int countByUserAndCreatedAtAfter(User user, LocalDateTime since);

    List<EmailVerificationToken> findByUserAndCreatedAtAfterOrderByCreatedAtDesc(
            User user, LocalDateTime since);



}
