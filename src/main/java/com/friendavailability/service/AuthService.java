package com.friendavailability.service;

import com.friendavailability.model.User;
import com.friendavailability.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<User> authenticateUser(String email, String rawPassword){
        System.out.println("Authenticating user: " + email);
        try {
            if (email == null || email.trim().isEmpty()) {
                System.out.println("Email is empty");
                return Optional.empty();
            }
            if (rawPassword == null || rawPassword.isEmpty()) {
                System.out.println("Password is required");
                return Optional.empty();
            }
            Optional<User> userOptional = userService.findUserByEmail(email.trim().toLowerCase());

            if (userOptional.isEmpty()) {
                System.out.println("User not found with email: " + email);
                passwordEncoder.encode("prevent_timing_attacks");
                return Optional.empty();
            }

            User user = userOptional.get();

            if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
                System.out.println("OAuth user");
                return Optional.empty();
            }

            if (!user.getIsActive()) {
                System.out.println("Account disabled for " + email);
                return Optional.empty();
            }

            boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPasswordHash());

            if (passwordMatches) {
                System.out.println("Authentication successful for " + email);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
                return Optional.of(user);
            } else {
                System.out.println("Authentication failed for " + email);
                return Optional.empty();
            }
        }catch(Exception e){
            System.out.println("Authentication error for " + email + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public User createUser(String name, String email, String rawPassword){
        System.out.println("Creating new account for " + email);

        try{
            validateUserRegistrationInput(name, email, rawPassword);
            String normalisedEmail = email.trim().toLowerCase();

            if(userService.findUserByEmail(normalisedEmail).isPresent()){
                throw new IllegalArgumentException("User with email " + normalisedEmail + " already exists");
            }

            String hashedPassword = passwordEncoder.encode(rawPassword);
            System.out.println("Password hashed successfully for " + normalisedEmail);

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

            System.out.println("User created successfully: " + savedUser.getName() + " (ID: " + savedUser.getId() + ")");

            return savedUser;

        } catch (IllegalArgumentException e) {
            System.err.println("User creation validation error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("User creation error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create user account: " + e.getMessage());
        }
    }

    private void validateUserRegistrationInput(String name, String email, String rawPassword) {
        // Name validation
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (name.trim().length() < 2) {
            throw new IllegalArgumentException("Name must be at least 2 characters long");
        }
        if (name.trim().length() > 50) {
            throw new IllegalArgumentException("Name cannot exceed 50 characters");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!isValidEmail(email.trim())) {
            throw new IllegalArgumentException("Please provide a valid email address");
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (rawPassword.length() > 100) {
            throw new IllegalArgumentException("Password cannot exceed 100 characters");
        }
        if (!isPasswordStrong(rawPassword)) {
            System.out.println("Weak password detected for registration");
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

    public boolean changePassword(Long userId, String oldPassword, String newPassword ){
        System.out.println("Changing passwords for user " + userId);
        try{
            Optional<User> userOptional = userService.findUserById(userId);
            if(userOptional.isEmpty()){
                System.out.println("User not found with id " + userId);
                return false;
            }
            User user = userOptional.get();

            if(!passwordEncoder.matches(oldPassword, user.getPasswordHash())){
                System.out.println("Incorrect current password");
                return false;
            }
            validateUserRegistrationInput(user.getName(), user.getEmail(), newPassword);

            String hashedPassword = passwordEncoder.encode(newPassword);
            user.setPasswordHash(hashedPassword);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            System.out.println("Password changed successfully for user: " + user.getName());
            return true;

        } catch (Exception e) {
            System.err.println("Password change error: " + e.getMessage());
            return false;
        }
    }

    public boolean isEmailAvailable(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        return userService.findUserByEmail(normalizedEmail).isEmpty();
    }
}
