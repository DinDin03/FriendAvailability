package com.friendavailability.controller;

import com.friendavailability.dto.auth.AuthRequest;
import com.friendavailability.dto.auth.AuthResponse;
import com.friendavailability.dto.auth.UserDto;
import com.friendavailability.model.User;
import com.friendavailability.service.UserService;
import com.friendavailability.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:8080", "http://127.0.0.1:8080"})
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest loginRequest,
            HttpServletRequest httpRequest) {

        try {
            System.out.println("Login attempt for: " + loginRequest.getEmail());

            Optional<User> authenticatedUser = authService.authenticateUser(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
            );

            if (authenticatedUser.isPresent()) {
                User user = authenticatedUser.get();

                HttpSession session = httpRequest.getSession(true);
                session.setAttribute("user_id", user.getId());
                session.setAttribute("user_email", user.getEmail());
                session.setAttribute("authenticated", true);

                System.out.println("Login successful for: " + user.getName() + " (ID: " + user.getId() + ")");
                AuthResponse response = AuthResponse.success(
                        "Welcome back! Login successful.",
                        UserDto.fromUser(user),
                        "/dashboard.html"
                );

                return ResponseEntity.ok(response);

            } else {
                System.out.println("Login failed for: " + loginRequest.getEmail());

                AuthResponse response = AuthResponse.error(
                        "Invalid email or password. Please try again.",
                        "INVALID_CREDENTIALS"
                );

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            AuthResponse response = AuthResponse.error("An error occurred during login. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody AuthRequest registerRequest,
            HttpServletRequest httpRequest) {

        try {
            System.out.println("Registration attempt for: " + registerRequest.getEmail());
            if (!registerRequest.isRegistration()) {
                AuthResponse response = AuthResponse.error(
                        "Name is required for registration",
                        "VALIDATION_ERROR"
                );
                return ResponseEntity.badRequest().body(response);
            }

            Optional<User> existingUser = userService.findUserByEmail(registerRequest.getEmail());
            if (existingUser.isPresent()) {
                AuthResponse response = AuthResponse.error(
                        "An account with this email already exists. Try logging in instead.",
                        "EMAIL_EXISTS"
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            User newUser = authService.createUser(
                    registerRequest.getName(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword()
            );

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("user_id", newUser.getId());
            session.setAttribute("user_email", newUser.getEmail());
            session.setAttribute("authenticated", true);

            System.out.println("Registration successful for: " + newUser.getName() + " (ID: " + newUser.getId() + ")");

            AuthResponse response = AuthResponse.success(
                    "Welcome to LinkUp! Your account has been created successfully.",
                    UserDto.fromUser(newUser),
                    "/dashboard.html"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            System.err.println("Registration validation error: " + e.getMessage());

            AuthResponse response = AuthResponse.error(e.getMessage(), "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            System.err.println("Registration error: " + e.getMessage());

            AuthResponse response = AuthResponse.error("An error occurred during registration. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/current-user")
    public ResponseEntity<UserDto> getCurrentUser(HttpServletRequest request) {
        try {
            System.out.println("🔍 Checking current user authentication");

            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("authenticated") != null) {
                Long userId = (Long) session.getAttribute("user_id");
                if (userId != null) {
                    Optional<User> user = userService.findUserById(userId);
                    if (user.isPresent()) {
                        System.out.println("Found session user: " + user.get().getName());
                        return ResponseEntity.ok(UserDto.fromUser(user.get()));
                    }
                }
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();

                if (principal instanceof OidcUser) {
                    OidcUser oidcUser = (OidcUser) principal;
                    String googleId = oidcUser.getSubject();

                    Optional<User> user = userService.findUserByGoogleId(googleId);
                    if (user.isPresent()) {
                        System.out.println("Found OAuth user: " + user.get().getName());
                        return ResponseEntity.ok(UserDto.fromUser(user.get()));
                    }
                }
            }

            System.out.println("No authenticated user found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        } catch (Exception e) {
            System.err.println("Error checking current user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        try {
            System.out.println("Processing logout request");

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
                System.out.println("Session invalidated");
            }

            SecurityContextHolder.clearContext();

            Map<String, String> response = new HashMap<>();
            response.put("message", "You have been successfully logged out");
            response.put("redirectUrl", "/");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Logout error: " + e.getMessage());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Logout failed");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmailAvailability(@RequestParam String email) {
        try {
            boolean available = authService.isEmailAvailable(email);

            Map<String, Boolean> response = new HashMap<>();
            response.put("available", available);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Email check error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            System.out.println("Password reset requested for: " + email);

            Map<String, String> response = new HashMap<>();
            response.put("message", "If an account with this email exists, you will receive password reset instructions shortly.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Password reset error: " + e.getMessage());

            Map<String, String> response = new HashMap<>();
            response.put("message", "An error occurred. Please try again later.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        try {
            HttpSession session = httpRequest.getSession(false);
            if (session == null || session.getAttribute("user_id") == null) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Authentication required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Long userId = (Long) session.getAttribute("user_id");
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            boolean success = authService.changePassword(userId, oldPassword, newPassword);

            Map<String, String> response = new HashMap<>();
            if (success) {
                response.put("message", "Password changed successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Failed to change password. Please check your current password.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            System.err.println("Password change error: " + e.getMessage());

            Map<String, String> response = new HashMap<>();
            response.put("message", "An error occurred while changing password");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}