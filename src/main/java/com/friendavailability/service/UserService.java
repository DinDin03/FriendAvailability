package com.friendavailability.service;

import com.friendavailability.model.User;
import com.friendavailability.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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

    public Optional<User> findUserById(Long id){
        System.out.println("Finding user with ID: " + id);
        Optional<User> user = userRepository.findById(id);

        if(user.isPresent()){
            System.out.println("Found user: " + user.get());
        }
        else{
            System.out.println("User not found with id: " + id);
        }
        return user;
    }

    public Optional<User> findUserByEmail(String email){
        System.out.println("Finding user with Email: " + email);
        Optional<User> user = userRepository.findByEmail(email);

        if(user.isPresent()){
            System.out.println("Found user " + user.get());
        }
        else{
            System.out.println("User not found with email: " + email);
        }
        return user;
    }

    public Optional<User> findUserByGoogleId(String googleId) {
        System.out.println("Finding user by Google ID: " + googleId);
        Optional<User> user = userRepository.findByGoogleId(googleId);

        if (user.isPresent()) {
            System.out.println("Found user: " + user.get());
        } else {
            System.out.println("User not found with Google ID: " + googleId);
        }

        return user;
    }

    public Optional<User> updateUser(Long id, String name, String email){
        System.out.println("Updating user: id = " + id + " name = " + name + " email = " + email);
        Optional<User> userOpt = userRepository.findById(id);

        if(userOpt.isEmpty()){
            System.out.println("User not found with id: " + id);
            return Optional.empty();
        }

        User user = userOpt.get();

        if(email != null && !user.getEmail().equals(email)){
            if(userRepository.existsByEmail(email)){
                throw new RuntimeException("User with email already exists");
            }
            user.setEmail(email);
        }

        if(name != null && !name.trim().isEmpty()){
            user.setName(name);
        }

        User updatedUser = userRepository.save(user);
        System.out.println("User updated: " + updatedUser);
        return Optional.of(updatedUser);
    }

    public Optional<User> linkGoogleAccount(Long userId, String googleId){
        System.out.println("Linking googleId " + googleId + " for user with id " + userId);

        if(userRepository.existsByGoogleId(googleId)){
            throw new RuntimeException("Google account is already linked to another user");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isPresent()){
            User user = userOpt.get();
            user.setGoogleId(googleId);
            user.setEmailVerified(true);

            User updatedUser = userRepository.save(user);
            System.out.println("Linked google account " + updatedUser);
            return Optional.of(updatedUser);
        }
        return Optional.empty();
    }

    public boolean deleteUserById(Long id){
        System.out.println("Deleting user with id: " + id);
        Optional<User> user = userRepository.findById(id);

        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            System.out.println("Deleted user with id: " + id);
            return true;
        } else {
            System.out.println("User not found with id: " + id);
            return false;
        }
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