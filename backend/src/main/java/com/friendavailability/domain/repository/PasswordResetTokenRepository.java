package com.friendavailability.domain.repository;

import com.friendavailability.domain.entity.token.PasswordResetToken;
import com.friendavailability.domain.entity.User;
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
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUserOrderByCreatedAtDesc(User user);

    Optional<PasswordResetToken> findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(User user, LocalDateTime now);

    boolean existsByUserAndUsedFalseAndExpiresAtAfter(User user, LocalDateTime now);

    List<PasswordResetToken> findByExpiresAtBefore(LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.user = :user")
    int deletedByUser(@Param("user") User user);

    int countByUserAndCreatedAtAfter(User user, LocalDateTime since);

    List<PasswordResetToken> findByUserAndCreatedAtAfterOrderByCreatedAtDesc(User user, LocalDateTime since);

}
