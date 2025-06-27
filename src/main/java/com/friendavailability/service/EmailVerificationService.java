package com.friendavailability.service;

import com.friendavailability.model.EmailVerificationToken;
import com.friendavailability.model.User;
import com.friendavailability.repository.EmailVerificationTokenRepository;
import com.friendavailability.repository.UserRepository;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    public String createVerificationToken(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getEmailVerified()) {
            throw new IllegalStateException("User email is already verified");
        }

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        int recentTokenCount = tokenRepository.countByUserAndCreatedAtAfter(user, oneHourAgo);

        if (recentTokenCount >= 3) {
            throw new IllegalStateException(
                    "Too many verification email requests. Please wait before requesting another email."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<EmailVerificationToken> existingToken =
                tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(user, now);

        if (existingToken.isPresent()) {
            System.out.println("User " + user.getEmail() + " already has valid verification token");
            return existingToken.get().getToken();
        }

        String tokenValue = UUID.randomUUID().toString();

        while (tokenRepository.findByToken(tokenValue).isPresent()) {
            tokenValue = UUID.randomUUID().toString();
            System.out.println("Token collision detected, generating new token");
        }

        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .token(tokenValue)
                .used(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        EmailVerificationToken savedToken = tokenRepository.save(token);

        System.out.println("Created verification token for user: " + user.getEmail() +
                " (Token ID: " + savedToken.getId() + ")");

        return tokenValue;
    }

    public VerificationResult verifyEmail(String tokenValue) {
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            return VerificationResult.failure("Invalid verification token");
        }

        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(tokenValue.trim());

        if (tokenOpt.isEmpty()) {
            System.out.println("Verification attempted with non-existent token: " + tokenValue);
            return VerificationResult.failure("Verification token not found. The link may be invalid or expired.");
        }

        EmailVerificationToken token = tokenOpt.get();
        User user = token.getUser();

        if (user.getEmailVerified()) {
            System.out.println("Verification attempted for already verified user: " + user.getEmail());
            return VerificationResult.success("Email is already verified", user);
        }

        if (token.isUsed()) {
            System.out.println("Verification attempted with already used token for user: " + user.getEmail());
            return VerificationResult.failure("This verification link has already been used. Please request a new verification email if needed.");
        }

        if (token.isExpired()) {
            System.out.println("Verification attempted with expired token for user: " + user.getEmail());
            return VerificationResult.failure("This verification link has expired. Please request a new verification email.");
        }

        try {
            user.setEmailVerified(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            token.markAsUsed();
            tokenRepository.save(token);

            System.out.println("Email verification successful for user: " + user.getEmail() +
                    " (User ID: " + user.getId() + ")");

            return VerificationResult.success("Email verified successfully! You can now log in to your account.", user);

        } catch (Exception e) {
            System.err.println("Error during email verification for user: " + user.getEmail() + " - " + e.getMessage());
            return VerificationResult.failure("An error occurred during verification. Please try again or contact support.");
        }
    }

    public boolean hasValidVerificationToken(User user) {
        if (user == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return tokenRepository.existsByUserAndUsedFalseAndExpiresAtAfter(user, now);
    }

    public VerificationStatus getVerificationStatus(User user) {
        if (user == null) {
            return new VerificationStatus(false, false, null, "User not found");
        }

        if (user.getEmailVerified()) {
            return new VerificationStatus(true, false, null, "Email is verified");
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<EmailVerificationToken> validToken =
                tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(user, now);

        if (validToken.isPresent()) {
            EmailVerificationToken token = validToken.get();
            return new VerificationStatus(false, true, token.getExpiresAt(),
                    "Verification email sent. Please check your inbox and click the verification link.");
        } else {
            return new VerificationStatus(false, false, null,
                    "Email not verified. Please request a verification email.");
        }
    }

    public int cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = tokenRepository.deleteExpiredTokens(now);

        if (deletedCount > 0) {
            System.out.println("Cleaned up " + deletedCount + " expired verification tokens");
        }

        return deletedCount;
    }

    public String resendVerificationEmail(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getEmailVerified()) {
            throw new IllegalStateException("User email is already verified");
        }

        String tokenValue = createVerificationToken(user);

        System.out.println("Resending verification email for user: " + user.getEmail());

        return tokenValue;
    }

    @Getter
    public static class VerificationResult {
        private final boolean success;
        private final String message;
        private final User user;

        private VerificationResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public static VerificationResult success(String message, User user) {
            return new VerificationResult(true, message, user);
        }

        public static VerificationResult failure(String message) {
            return new VerificationResult(false, message, null);
        }

        @Override
        public String toString() {
            return "VerificationResult{success=" + success + ", message='" + message + "'}";
        }
    }

    public record VerificationStatus(@Getter boolean isVerified, boolean hasPendingToken,
                                     @Getter LocalDateTime tokenExpiresAt, @Getter String message) {

        @Override
            public String toString() {
                return "VerificationStatus{" +
                        "isVerified=" + isVerified +
                        ", hasPendingToken=" + hasPendingToken +
                        ", tokenExpiresAt=" + tokenExpiresAt +
                        ", message='" + message + '\'' +
                        '}';
            }
        }
}