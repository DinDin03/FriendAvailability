package com.linkups.domain.service;

import com.linkups.api.dto.request.auth.AuthRequest;
import com.linkups.domain.entity.User;
import com.linkups.domain.exception.*;
import com.linkups.domain.repository.UserRepository;
import com.linkups.api.dto.response.auth.GoogleUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
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

    @org.springframework.transaction.annotation.Transactional
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

        User user = userService.findUserByEmail(normalisedEmail);

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

    @org.springframework.transaction.annotation.Transactional
    public User registerUser(AuthRequest registerRequest){
        String name = registerRequest.getName();
        String email = registerRequest.getEmail();
        String password = registerRequest.getPassword();

        log.info("Processing registration for email: {}", email);

        validateUserRegistrationInput(name, email, password);

        String normalisedEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmail(normalisedEmail)) {
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
                User user = userService.findUserById(userId);
                log.debug("Found session authenticated user: {}", user.getId());
                return user;
            }
        }

        log.warn("No authenticated user found");
        throw ResourceNotFoundException.noAuthenticatedUser();
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

    @org.springframework.transaction.annotation.Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("Processing password change for user: {}", userId);

        User user = userService.findUserById(userId);

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            log.warn("Incorrect current password for user: {}", userId);
            throw ValidationException.invalidPassword("Current password is incorrect");
        }

        validateUserRegistrationInput(user.getName(), user.getEmail(), newPassword);

        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashedPassword);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", userId);
        return true;
    }

    public boolean isEmailAvailable(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        try {
            userService.findUserByEmail(normalizedEmail);
            log.debug("Email availability check for {}: false", normalizedEmail);
            return false;
        } catch (ResourceNotFoundException e) {
            log.debug("Email availability check for {}: true", normalizedEmail);
            return true;
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }

    public boolean isPasswordStrong(String password) {
        return password != null && password.length() >= 8;
    }

    @org.springframework.transaction.annotation.Transactional
    public User authenticateWithGoogleJwt(String credential, HttpServletRequest request) {
        GoogleUserInfo userInfo = googleJwtVerificationService.verifyToken(credential);

        User user = processGoogleUser(userInfo.getGoogleId(), userInfo.getEmail(), userInfo.getName());

        createUserSession(request, user);

        return user;
    }

    private User processGoogleUser(String googleId, String email, String name) {
        // Check if user exists by Google ID
        Optional<User> userByGoogleIdOpt = userService.findUserByGoogleIdOptional(googleId);
        if (userByGoogleIdOpt.isPresent()) {
            return userByGoogleIdOpt.get();
        }

        // Check if user exists by email
        Optional<User> userByEmailOpt = userService.findUserByEmailOptional(email);
        if (userByEmailOpt.isPresent()) {
            User existingUser = userByEmailOpt.get();
            if (existingUser.getGoogleId() == null) {
                return userService.linkGoogleAccount(existingUser.getId(), googleId);
            }
            return existingUser;
        }

        // Create new user with Google
        return userService.createUserWithGoogle(name, email, googleId);
    }

    private void createUserSession(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        session.setAttribute("authenticated", true);
        session.setAttribute("user_id", user.getId());
        session.setAttribute("user_email", user.getEmail());
    }
}