package com.friendavailability.domain.service;

import com.friendavailability.api.dto.request.auth.AuthRequest;
import com.friendavailability.api.dto.response.auth.AuthResponse;
import com.friendavailability.domain.entity.User;
import com.friendavailability.domain.exception.*;
import com.friendavailability.domain.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder){
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

   public User authenticatedAndLogin(AuthRequest loginRequest, HttpServletRequest httpRequest){
        log.info("Processing login attempt for email: {}", loginRequest.getEmail());

       String email = loginRequest.getEmail();
       String password = loginRequest.getPassword();

       if (email == null || email.trim().isEmpty()) {
           log.warn("Login attempt with empty email");
           throw ValidationException.requiredEmail();
       }
       if (password == null || password.isEmpty()) {
           log.warn("Login attempt with empty password for email: {}", email);
           throw ValidationException.requiredPassword();
       }

       String normalisedEmail = email.trim().toLowerCase();

       Optional<User> userOptional = userService.findUserByEmail(normalisedEmail);
       if(userOptional.isEmpty()){
           log.warn("Login attempt for non-existent email: {}", normalisedEmail);
           passwordEncoder.encode("prevent_timing_attacks");
           throw ResourceNotFoundException.userEmailNotFound(normalisedEmail);
       }

       User user = userOptional.get();

       if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
           log.warn("Login attempt for OAuth user with password: {}", normalisedEmail);
           throw new InvalidOperationException("This account uses Google login. Please use the Google sign-in button.");
       }
   }
}
