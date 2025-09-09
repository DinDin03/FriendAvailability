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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleJwtVerificationService googleJwtVerificationService;

    public AuthService(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder,
                       GoogleJwtVerificationService googleJwtVerificationService){
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.googleJwtVerificationService = googleJwtVerificationService;
    }

   public User authenticateAndLogin(AuthRequest loginRequest, HttpServletRequest httpRequest){
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
           throw InvalidOperationException.googleUserUsingPassword(normalisedEmail);
       }

       if(!user.getEmailVerified()){
           log.warn("Login attempt for unverified email: {}", normalisedEmail);
           throw ValidationException.loginWithUnverifiedEmail(normalisedEmail);
       }

       if(!user.getIsActive()){
           log.warn("Login attempt for disabled account: {}", normalisedEmail);
           throw ValidationException.loginWithDisabledAccount(normalisedEmail);
       }

       boolean passwordMatches = passwordEncoder.matches(password, user.getPasswordHash());
       if(!passwordMatches){
           log.warn("Invalid password attempt for email: {}", normalisedEmail);
           throw ValidationException.incorrectCredentials();
       }

       user.setUpdatedAt(LocalDateTime.now());
       userRepository.save(user);

       HttpSession session = httpRequest.getSession();
       session.setAttribute("authenticated", true);
       session.setAttribute("user_id", user.getId());

       log.info("Successful authentication for user: {} with ID: {}", user.getEmail(), user.getId());

       return user;
   }

   public User registerUser(AuthRequest registerRequest){
        String name = registerRequest.getName();
        String email = registerRequest.getEmail();
        String password = registerRequest.getPassword();

        log.info("Processing registration for email: {}", email);

        validateUserRegistrationInput(name, email, password);

        String normalisedEmail = email.trim().toLowerCase();

        if(userService.findUserByEmail(normalisedEmail).isPresent()){
            log.warn("Registration attempt for existing email: {}", normalisedEmail);
            throw DuplicateResourceException.duplicateEmail(normalisedEmail);
        }

        String hashedPassword = passwordEncoder.encode(password);
        log.debug("Password hashed successfully for email: {}", normalisedEmail);

       User newUser = User.builder()
               .name(name.trim())
               .email(normalisedEmail)
               .passwordHash(hashedPassword)
               .isActive(true)
               .emailVerified(false)
               .createdAt(LocalDateTime.now())
               .updatedAt(LocalDateTime.now())
               .build();

       User savedUser = userRepository.save(newUser);

       log.info("User registered successfully for email: {} with ID: {}", savedUser.getEmail(), savedUser.getId());
       return savedUser;
   }

    public User getCurrentAuthenticatedUser(HttpServletRequest request) {
        log.debug("Checking current user authentication");

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("authenticated") != null) {
            Long userId = (Long) session.getAttribute("user_id");
            if (userId != null) {
                Optional<User> user = userService.findUserById(userId);
                if (user.isPresent()) {
                    log.debug("Found session authenticated user: {}", user.get().getId());
                    return user.get();
                }
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof OidcUser oidcUser) {
                String googleId = oidcUser.getSubject();

                Optional<User> user = userService.findUserByGoogleId(googleId);
                if (user.isPresent()) {
                    log.debug("Found OAuth-authenticated user: {}", user.get().getId());
                    return user.get();
                }
            }
        }

        log.warn("No authenticated user found");
        throw new ResourceNotFoundException("No authenticated user found");
    }


    private void validateUserRegistrationInput(String name, String email, String rawPassword) {
        if (name == null || name.trim().isEmpty()) {
            throw ValidationException.requiredName();
        }
        if (name.trim().length() < 2) {
            throw ValidationException.nameTooShort(2);
        }
        if (name.trim().length() > 50) {
            throw ValidationException.nameTooLong(50, name.trim().length());
        }

        if (email == null || email.trim().isEmpty()) {
            throw ValidationException.requiredEmail();
        }
        if (!isValidEmail(email.trim())) {
            throw ValidationException.invalidEmail(email.trim());
        }

        // Password validation using convenience methods
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw ValidationException.requiredPassword();
        }
        if (rawPassword.length() < 8) {
            throw ValidationException.passwordTooShort(8);
        }
        if (rawPassword.length() > 100) {
            throw ValidationException.passwordTooLong(100);
        }
        if (!isPasswordStrong(rawPassword)) {
            log.warn("Weak password detected during registration");
            throw ValidationException.passwordTooWeak();
        }
    }

    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("Processing password change for user: {}", userId);

        try {
            Optional<User> userOptional = userService.findUserById(userId);
            if (userOptional.isEmpty()) {
                log.warn("Password change attempt for non existent user: {}", userId);
                throw new ResourceNotFoundException("User", userId);
            }

            User user = userOptional.get();

            if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                log.warn("Incorrect current password for user: {}", userId);
                throw new ValidationException("Current password is incorrect");
            }

            validateUserRegistrationInput(user.getName(), user.getEmail(), newPassword);

            String hashedPassword = passwordEncoder.encode(newPassword);
            user.setPasswordHash(hashedPassword);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Password changed successfully for user: {}", userId);
            return true;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during password change for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to change password: " + e.getMessage());
        }
    }

    public boolean isEmailAvailable(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        boolean available = userService.findUserByEmail(normalizedEmail).isEmpty();
        log.debug("Email availability check for {}: {}", normalizedEmail, available);
        return available;
    }
    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }

    public boolean isPasswordStrong(String password) {
        return password != null && password.length() >= 8;
    }

    public User authenticateWithGoogleJwt(String credential, HttpServletRequest request) {
        GoogleJwtVerificationService.GoogleUserInfo userInfo = googleJwtVerificationService.verifyToken(credential);

        User user = processGoogleUser(userInfo.getGoogleId(), userInfo.getEmail(), userInfo.getName());

        createUserSession(request, user);

        return user;
    }

    private User processGoogleUser(String googleId, String email, String name) {
        Optional<User> userByGoogleId = userService.findUserByGoogleId(googleId);
        if (userByGoogleId.isPresent()) {
            return userByGoogleId.get();
        }

        Optional<User> userByEmail = userService.findUserByEmail(email);
        if (userByEmail.isPresent()) {
            User user = userByEmail.get();
            if (user.getGoogleId() == null) {
                return userService.linkGoogleAccount(user.getId(), googleId).orElse(user);
            }
            return user;
        }

        return userService.createUserWithGoogle(name, email, googleId);
    }

    private void createUserSession(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        session.setAttribute("authenticated", true);
        session.setAttribute("user_id", user.getId());
        session.setAttribute("user_email", user.getEmail());
    }
}
