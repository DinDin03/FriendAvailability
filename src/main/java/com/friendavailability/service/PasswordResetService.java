package com.friendavailability.service;

import com.friendavailability.model.PasswordResetToken;
import com.friendavailability.model.User;
import com.friendavailability.repository.PasswordResetTokenRepository;
import com.friendavailability.repository.UserRepository;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService){
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public String createResetToken(User user){
        if(user == null) throw new IllegalArgumentException("User cannot be null");
        if(!user.getEmailVerified()) throw new IllegalStateException("User email needs to be verified first");

        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        int recentTokens = tokenRepository.countByUserAndCreatedAtAfter(user, thirtyMinutesAgo);

        if (recentTokens > 3) throw new IllegalStateException("Too many reset password attempts " +
                "try again in 30 minutes");

        LocalDateTime now = LocalDateTime.now();
        Optional<PasswordResetToken> existingToken = tokenRepository
                .findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(user, now);

        if(existingToken.isPresent()){
            System.out.println("User " + user.getEmail() + " already has a valid recent password token");
            return existingToken.get().getToken();
        }

        String tokenValue = UUID.randomUUID().toString();

        while(tokenRepository.findByToken(tokenValue).isPresent()){
            tokenValue = UUID.randomUUID().toString();
        }

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(tokenValue)
                .used(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        PasswordResetToken savedToken = tokenRepository.save(token);

        return tokenValue;
    }

    public boolean resetPassword(String tokenValue, String newPassword){
        if(newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if(newPassword.length() < 8){
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(tokenValue);
        if(tokenOpt.isEmpty()){
            System.out.println("Token not found");
            return false;
        }
        PasswordResetToken token = tokenOpt.get();

        if(!token.isValid()){
            System.out.println("Token is either used or expired");
            return false;
        }

        try {
            User user = token.getUser();
            String newHashedPassword = passwordEncoder.encode(newPassword);
            user.setPasswordHash(newHashedPassword);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            token.setUsed(true);
            tokenRepository.save(token);
            return true;
        }catch(Exception e){
            System.out.println("An error occured " + e.getMessage());
            return false;
        }
    }

    public boolean requestPasswordReset(String email){
        if(email == null || email.trim().isEmpty()){
            return false;
        }

        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase().trim());

        if(userOpt.isEmpty()) {
            System.out.println("Password reset requested for non-existent email: " + email);
            return true;
        }

        User user = userOpt.get();

        if(!user.getEmailVerified()) {
            System.out.println("Password reset requested for unverified email: " + email);
            return true;
        }

        try{
            String token = createResetToken(user);
            return emailService.sendPasswordResetEmail(user, token);
        }catch (Exception e) {
            System.err.println("Password reset failed for " + email + ": " + e.getMessage());
            return false;
        }
    }

    public Optional<User> validateResetToken(String tokenValue){
        if(tokenValue == null || tokenValue.trim().isEmpty()){
            return Optional.empty();
        }
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(tokenValue);

        if(tokenOpt.isEmpty()){
            return Optional.empty();
        }
        PasswordResetToken token = tokenOpt.get();

        if(!token.isValid()){
            return Optional.empty();
        }
        User user = token.getUser();
        return Optional.of(user);
    }

    public int cleanExpiredTokens(){
        int deletedCount = tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        if (deletedCount > 0) {
            System.out.println("Cleaned up " + deletedCount + " expired password reset tokens");
        }
        return deletedCount;
    }


}
