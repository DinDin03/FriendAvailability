package com.friendavailability.service;

import com.friendavailability.model.EmailVerificationToken;
import com.friendavailability.model.User;
import com.friendavailability.repository.EmailVerificationTokenRepository;
import com.friendavailability.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository, UserRepository userRepository){
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    public String createVerificationToken(User user){
        if(user == null) throw new IllegalArgumentException("User cannot be null");
        if(user.getEmailVerified()) throw new IllegalStateException("User email is already verified");

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        int recentTokenCount = tokenRepository.countByUserAndCreatedAtAfter(user, oneHourAgo);

        if(recentTokenCount > 3){
            throw new IllegalStateException("Too many verification attempts. Please try again in an hour");
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<EmailVerificationToken> existingToken = tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(user, now);

        if(existingToken.isPresent()){
            System.out.println("User " + user.getEmail() + " already has a valid verification token");
            return existingToken.get().getToken();
        }





    }

}
