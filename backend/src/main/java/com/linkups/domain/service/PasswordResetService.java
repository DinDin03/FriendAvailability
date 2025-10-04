package com.linkups.domain.service;

import com.linkups.domain.entity.token.PasswordResetToken;
import com.linkups.domain.entity.User;
import com.linkups.domain.exception.ResourceNotFoundException;
import com.linkups.domain.exception.ValidationException;
import com.linkups.domain.repository.PasswordResetTokenRepository;
import com.linkups.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        log.info("PasswordResetService initialized successfully");
    }

    public String createResetToken(User user) {
        log.debug("Creating password reset token for user: {}", user.getEmail());

        if (user == null) {
            log.error("Attempted to create reset token with null user");
            throw ValidationException.invalidFieldValue("user", "User cannot be null");
        }

        if (!user.getEmailVerified()) {
            log.warn("Password reset attempted for unverified email: {}", user.getEmail());
            throw ValidationException.passwordResetEmailUnverified(user.getEmail());
        }

        // Check rate limiting - only 3 attempts per 30 minutes
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        int recentTokens = tokenRepository.countByUserAndCreatedAtAfter(user, thirtyMinutesAgo);

        if (recentTokens > 3) {
            log.warn("Rate limit exceeded for password reset: {} (attempts: {})", user.getEmail(), recentTokens);
            throw ValidationException.passwordResetRateLimited(30);
        }

        // Check for existing valid token
        LocalDateTime now = LocalDateTime.now();
        Optional<PasswordResetToken> existingToken = tokenRepository
                .findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(user, now);

        if (existingToken.isPresent()) {
            log.debug("User {} already has a valid password reset token", user.getEmail());
            return existingToken.get().getToken();
        }

        // Generate unique token
        String tokenValue = generateUniqueToken();

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(tokenValue)
                .used(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        tokenRepository.save(token);

        log.info("Password reset token created successfully for user: {}", user.getEmail());
        return tokenValue;
    }

    public void resetPassword(String tokenValue, String newPassword) {
        log.info("Processing password reset for token: {}", tokenValue != null ? tokenValue.substring(0, 8) + "..." : "null");

        validatePasswordResetInput(tokenValue, newPassword);

        PasswordResetToken token = findValidToken(tokenValue);
        User user = token.getUser();

        // Update user password
        String newHashedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newHashedPassword);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark token as used
        token.setUsed(true);
        tokenRepository.save(token);

        log.info("Password reset completed successfully for user: {}", user.getEmail());
    }

    public void requestPasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);

        if (email == null || email.trim().isEmpty()) {
            throw ValidationException.passwordResetEmailRequired();
        }

        String normalizedEmail = email.toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        // For security reasons, we don't reveal if email exists or not
        // Always log the attempt but only send email if user exists and verified
        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", normalizedEmail);
            // Don't throw exception - return success to prevent email enumeration
            return;
        }

        User user = userOpt.get();

        if (!user.getEmailVerified()) {
            log.warn("Password reset requested for unverified email: {}", normalizedEmail);
            // Don't throw exception - return success to prevent information leakage
            return;
        }

        try {
            String token = createResetToken(user);
            boolean emailSent = emailService.sendPasswordResetEmail(user, token);

            if (!emailSent) {
                log.error("Failed to send password reset email to: {}", normalizedEmail);
                // Don't throw exception - for security, always appear successful
            } else {
                log.info("Password reset email sent successfully to: {}", normalizedEmail);
            }
        } catch (ValidationException e) {
            // Re-throw validation exceptions (rate limiting, etc.)
            throw e;
        } catch (Exception e) {
            log.error("Password reset failed for {}: {}", normalizedEmail, e.getMessage());
            // Don't throw exception - for security, always appear successful to user
        }
    }

    public User validateResetToken(String tokenValue) {
        log.debug("Validating password reset token: {}", tokenValue != null ? tokenValue.substring(0, 8) + "..." : "null");

        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            throw ValidationException.passwordResetInvalidToken();
        }

        PasswordResetToken token = findValidToken(tokenValue);
        User user = token.getUser();

        log.debug("Password reset token validated successfully for user: {}", user.getEmail());
        return user;
    }

    public int cleanExpiredTokens() {
        log.debug("Cleaning up expired password reset tokens");

        int deletedCount = tokenRepository.deleteExpiredTokens(LocalDateTime.now());

        if (deletedCount > 0) {
            log.info("Cleaned up {} expired password reset tokens", deletedCount);
        }

        return deletedCount;
    }

    private void validatePasswordResetInput(String tokenValue, String newPassword) {
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            throw ValidationException.passwordResetInvalidToken();
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw ValidationException.passwordResetRequiredPassword();
        }

        if (newPassword.length() < 8) {
            throw ValidationException.passwordResetTooShort(8);
        }
    }

    private PasswordResetToken findValidToken(String tokenValue) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(tokenValue);

        if (tokenOpt.isEmpty()) {
            log.warn("Password reset token not found: {}", tokenValue.substring(0, 8) + "...");
            throw ResourceNotFoundException.passwordResetTokenNotFound(tokenValue);
        }

        PasswordResetToken token = tokenOpt.get();

        if (token.isUsed()) {
            log.warn("Attempt to use already used password reset token for user: {}", token.getUser().getEmail());
            throw ResourceNotFoundException.passwordResetTokenUsed(tokenValue);
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Attempt to use expired password reset token for user: {}", token.getUser().getEmail());
            throw ResourceNotFoundException.passwordResetTokenExpired(tokenValue);
        }

        return token;
    }

    private String generateUniqueToken() {
        String tokenValue = UUID.randomUUID().toString();

        // Ensure token is unique
        while (tokenRepository.findByToken(tokenValue).isPresent()) {
            tokenValue = UUID.randomUUID().toString();
        }

        return tokenValue;
    }
}