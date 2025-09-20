package com.friendavailability.domain.service;

import com.friendavailability.domain.entity.User;
import com.friendavailability.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public User createUser(String name, String email) {
        System.out.println("Creating user: name=" + name + ", email=" + email);

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User with email " + email + " already exists");
        }

        User newUser = User.builder()
                .name(name)
                .email(email)
                .isActive(true)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(newUser);
        System.out.println("Created user: " + savedUser);
        return savedUser;
    }

    public User createUserWithPassword(String name, String email, String password){
        System.out.println("Creating user with password: name = " + name + " email = " + email);

        if(userRepository.existsByEmail(email)){
            throw new RuntimeException("User with email " + email + " already exists");
        }
        if(password == null || password.trim().length() < 8){
            throw new RuntimeException("Password must be at least 8 characters");
        }
        String hashedPassword = passwordEncoder.encode(password);

        User newUser = User.builder()
                .name(name)
                .email(email)
                .passwordHash(hashedPassword)
                .isActive(true)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(newUser);
        System.out.println("Created user with password " + savedUser);
        return savedUser;
    }

    public User createUserWithGoogle(String name, String email, String googleId) {
        System.out.println("Creating user with Google OAuth: name=" + name + ", email=" + email + ", googleId=" + googleId);

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User with email " + email + " already exists");
        }
        if (userRepository.existsByGoogleId(googleId)) {
            throw new RuntimeException("Google ID " + googleId + " is already linked to another user");
        }
        User newUser = User.builder()
                .name(name)
                .email(email)
                .googleId(googleId)
                .isActive(true)
                .emailVerified(true)
                .build();

        User savedUser = userRepository.save(newUser);
        System.out.println("Created Google OAuth user: " + savedUser);
        return savedUser;
    }

    public List<User> findAllUsers(){
        System.out.println("Finding all users");
        List<User> users = userRepository.findAll();
        System.out.println("Found " + users.size() + " users");
        return users;
    }

    public User findUserById(Long id){
        log.debug("Finding user with ID: {}", id);

        if(id == null || id <= 0){
            log.warn("Invalid user ID provided: {}", id);
            throw ValidationException.invalidUserId(id);
        }
        Optional<User> userOpt = userRepository.findById(id);

        if(userOpt.isEmpty()){
            log.warn("User not found with ID: {}", id);
            throw ResourceNotFoundException.userNotFound(id);
        }

        log.debug("Found user: {} ({})", userOpt.get().getName(), userOpt.get().getEmail());
        return userOpt.get();
    }

    public User findUserByEmail(String email){
        log.debug("Finding user with email: {}", email);

        if(email == null || email.trim().isEmpty()){
            log.warn("Invalid email provided");
            throw ValidationException.requiredEmail();
        }

        String normalisedEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findUserByEmail(normalisedEmail);

        if(userOpt.isEmpty()){
            log.warn("User not found with email: {}", normalisedEmail);
            throw ResourceNotFoundException.userEmailNotFound(normalisedEmail);
        }

        log.debug("Found user: {} ({})", userOpt.get().getName(), userOpt.get().getEmail());
        return userOpt.get();
    }

    public User findUserByGoogleId(String googleId) {
        log.debug("Finding user with google id: {}", googleId);

        if(googleId == null || googleId.trim().isEmpty()){
            log.warn("Empty google ID provided");
            throw ValidationException.invalidFieldValue("googleId", "Google ID cannot be empty");
        }

        Optional<User> userOpt = userRepository.findUserByGoogleId(googleId);
        
        if(userOpt.isEmpty()){
            log.warn("User not found with google ID: {}", googleId);
            throw ResourceNotFoundException.userGoogleIdNotFound(googleId);
        }
        log.debug("Found OAuth user: {} with email {}", userOpt.get().getName(), userOpt.get().getEmail());
        return userOpt.get();
    }

    public User updateUser(Long id, String name, String email){
        log.info("Updating user with ID: {}", id);

        User user = findUserById(id);

        if((name == null || name.trim().isEmpty()) && (email == null || email.trim().isEmpty())){
            log.warn("No valid data to update");
            throw ValidationException.noUserDataToUpdate();
        }

        boolean hasChanges = false;

        if(email != null && !email.trim().isEmpty()){
            String normalisedEmail = email.trim().toLowerCase(); 
            if(!user.getEmail().equals(normalisedEmail)){
                if(userRepository.existsByEmail(normalisedEmail)){
                    log.warn("Attempt to update email to an existing email {}", normalisedEmail);
                    throw DuplicateResourceException.duplicateUserEmail(normalisedEmail);
                }
                if(!isValidEmail(normalisedEmail)){
                    throw ValidationException.invalidEmail(normalisedEmail);
                }
                user.setEmail(normalisedEmail);
                hasChanges = true;
                log.debug("Email updated for user with id: {} ({})", id, normalisedEmail);
            }
        }

        if(name != null && name.trim().isEmpty()){
            String trimmedname = name.trim();
            if(!user.getName.equals(trimmedname)){
                if(trimmedname.length() < 2){
                    throw ValidationException.nameTooShort(2);
                }
                if(trimmedName.length() > 50){
                    throw ValidationException.nameTooLong(50, trimmedName.length());
                }

                user.setName(trimmedName);
                hasChanges = true;
                log.debug("Name changed to {} for user with id ", trimmedName, id);
            }
        }

        if(!hasChanges){
            log.debug("No changes detected for user {}", user.getEmail());
            return user;
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {} ({})", updatedUser.getName(), updatedUser.getEmail());
        return updatedUser;
    }

    public User linkGoogleAccount(Long userId, String googleId) {
        log.info("Linking Google account {} to user: {}", googleId, userId);
        
        if (googleId == null || googleId.trim().isEmpty()) {
            throw ValidationException.invalidFieldValue("googleId", "Google ID cannot be empty");
        }
        
        if (userRepository.existsByGoogleId(googleId)) {
            log.warn("Google account {} is already linked to another user", googleId);
            throw DuplicateResourceException.duplicateGoogleId(googleId);
        }
        
        User user = findUserById(userId);
        
        user.setGoogleId(googleId);
        user.setEmailVerified(true); 
        user.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(user);
        
        log.info("Google account linked successfully for user: {} ({})", 
                updatedUser.getName(), updatedUser.getEmail());
        return updatedUser;
    }

    public void deleteUserById(Long id){
        log.info("Deleting user with ID: {}", id)
        User user = findUserById(id);
        userRepository.deleteUserById(id);
        log.info("Deleted user with ID: {}", id);
    }

    public boolean validatePassword(User user, String password){
         if(user.getPasswordHash() == null){
             System.out.println("User " + user.getEmail() + " has no password (OAuth user)");
             return false;
         }
         boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
         System.out.println("Password verification for " + user.getEmail() + (matches ? "SUCCESS" : "FAILED"));
         return matches;
    }

    public boolean verifyUserEmail(Long id){
        Optional<User> userOpt = userRepository.findById(id);
        if(userOpt.isPresent()){
            User user = userOpt.get();
            user.setEmailVerified(true);
            userRepository.save(user);
            System.out.println("Verified email for user " + user.getEmail());
            return true;
        }
        return false;
    }

    public List<User> getActiveUsers(){
        System.out.println("Getting active users");
        List<User> activeUsers = userRepository.findByIsActiveTrue();
        System.out.println("Found " + activeUsers.size() + " active users");
        return activeUsers;
    }

    public List<User> searchUsersByName(String searchTerm){
        System.out.println("Searching users by name: " + searchTerm);
        List<User> users = userRepository.findByNameContainingIgnoreCase(searchTerm);
        System.out.println("Found " + users.size() + " users matching name: " + searchTerm);
        return users;
    }

    public List<User> getRecentUsers(int days){
        LocalDateTime cutOffDate = LocalDateTime.now().minusDays(days);
        System.out.println("Getting users registered after " + cutOffDate);
        List<User> users = userRepository.findRecentlyActiveUsers(cutOffDate);
        System.out.println("Found " + users.size() + " recent users");
        return users;
    }

    public Object[] getUserStatistics(){
        System.out.println("Getting user statistics");
        Object[] stats = userRepository.getUserStatistics();
        System.out.println("User statistics: Total=" + stats[0] + ", Active=" + stats[1] + ", Verified=" + stats[2]);
        return stats;
    }

    public boolean isEmailAvailable(String email){
        boolean available = !userRepository.existsByEmail(email);
        System.out.println(email + (available ? " available" : " not available"));
        return available;
    }

    public boolean isGoogleIdAvailable(String googleId){
        boolean available = !userRepository.existsByGoogleId(googleId);
        System.out.println(googleId + (available ? " available" : " not available"));
        return available;
    }
}