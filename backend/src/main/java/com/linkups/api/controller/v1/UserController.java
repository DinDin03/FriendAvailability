package com.linkups.api.controller.v1;

import com.linkups.api.dto.request.user.CreateUserRequest;
import com.linkups.api.dto.request.user.UpdateUserRequest;
import com.linkups.domain.entity.User;
import com.linkups.domain.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
        log.info("UserController initialized successfully");
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Getting all users");

        List<User> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        log.info("Getting user with ID: {}", id);

        User user = userService.findUserById(id);  // Throws ResourceNotFoundException if not found
        return ResponseEntity.ok(user);
    }

    @GetMapping("/by-email")
    public ResponseEntity<User> getUserByEmail(@RequestParam String email) {
        log.info("Getting user with email: {}", email);

        User user = userService.findUserByEmail(email);  // Throws ResourceNotFoundException if not found
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Creating user: {}", request.getEmail());

        User user;
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user = userService.createUserWithPassword(
                    request.getName(),
                    request.getEmail(),
                    request.getPassword()
            );
        } else {
            user = userService.createUser(request.getName(), request.getEmail());
        }

        log.info("User created successfully: {} (ID: {})", user.getEmail(), user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id,
                                           @Valid @RequestBody UpdateUserRequest request) {
        log.info("Updating user with ID: {}", id);

        User updatedUser = userService.updateUser(id, request.getName(), request.getEmail());

        log.info("User updated successfully: {} (ID: {})", updatedUser.getEmail(), updatedUser.getId());
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with ID: {}", id);

        userService.deleteUserById(id);  // Throws ResourceNotFoundException if not found

        log.info("User deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/link-google")
    public ResponseEntity<User> linkGoogleAccount(@PathVariable Long id,
                                                  @RequestParam String googleId) {
        log.info("Linking Google account to user: {}", id);

        User updatedUser = userService.linkGoogleAccount(id, googleId);

        log.info("Google account linked successfully for user: {}", id);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/active")
    public ResponseEntity<List<User>> getActiveUsers() {
        log.info("Getting active users");

        List<User> activeUsers = userService.getActiveUsers();
        return ResponseEntity.ok(activeUsers);
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsersByName(@RequestParam String name) {
        log.info("Searching users by name: {}", name);

        List<User> users = userService.searchUsersByName(name);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/stats")
    public ResponseEntity<Object[]> getUserStatistics() {
        log.info("Getting user statistics");

        Object[] stats = userService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/email-available")
    public ResponseEntity<Boolean> isEmailAvailable(@RequestParam String email) {
        log.debug("Checking email availability: {}", email);

        boolean available = userService.isEmailAvailable(email);
        return ResponseEntity.ok(available);
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getCurrentUserProfile(@RequestParam Long userId) {
        log.info("Getting current user profile for user ID: {}", userId);

        User user = userService.findUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateCurrentUserProfile(@RequestParam Long userId,
                                                         @Valid @RequestBody UpdateUserRequest request) {
        log.info("Updating current user profile for user ID: {}", userId);

        User updatedUser = userService.updateUser(userId, request.getName(), request.getEmail());

        log.info("User profile updated successfully: {} (ID: {})", updatedUser.getEmail(), updatedUser.getId());
        return ResponseEntity.ok(updatedUser);
    }
}