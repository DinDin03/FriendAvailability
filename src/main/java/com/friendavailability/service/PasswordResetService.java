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

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    public boolean ResetPassword(User user, String oldPassword, String newPassword){
        if(newPassword == null || newPassword.) {
            throw new IllegalArgumentException("Password cannot be null");
            return false;
        }

    }

}
