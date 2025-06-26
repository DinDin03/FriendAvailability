package com.friendavailability.controller;

import com.friendavailability.dto.auth.AuthRequest;
import com.friendavailability.dto.auth.AuthResponse;
import com.friendavailability.dto.auth.UserDto;
import com.friendavailability.model.User;
import com.friendavailability.service.EmailService;
import com.friendavailability.service.EmailVerificationService;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:8080", "http://127.0.0.1:8080", "https://friendavailability-production.up.railway.app"})
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest loginRequest,
            HttpServletRequest httpRequest) {

        try {
            System.out.println("Login attempt for: " + loginRequest.getEmail());

            Optional<User> userOpt = userService.findUserByEmail(loginRequest.getEmail());
            if (userOpt.isPresent() && !userOpt.get().getEmailVerified()) {
                AuthResponse response = AuthResponse.error(
                        "Please verify your email before logging in. Check your inbox for the verification link.",
                        "EMAIL_NOT_VERIFIED"
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

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
                User user = existingUser.get();

                if (!user.getEmailVerified()) {
                    try {
                        String token = emailVerificationService.resendVerificationEmail(user);
                        boolean emailSent = emailService.sendVerificationEmail(user, token);

                        if (emailSent) {
                            AuthResponse response = AuthResponse.success(
                                    "An account with this email already exists but is not verified. " +
                                            "We've sent a new verification email. Please check your inbox."
                            );
                            return ResponseEntity.ok(response);
                        } else {
                            AuthResponse response = AuthResponse.error(
                                    "Account exists but verification email could not be sent. Please try again.",
                                    "EMAIL_SEND_FAILED"
                            );
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                        }
                    } catch (IllegalStateException e) {
                        AuthResponse response = AuthResponse.error(e.getMessage(), "RATE_LIMITED");
                        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
                    }
                } else {
                    AuthResponse response = AuthResponse.error(
                            "An account with this email already exists and is verified. Try logging in instead.",
                            "EMAIL_EXISTS"
                    );
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }
            }

            User newUser = authService.createUser(
                    registerRequest.getName(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword()
            );

            String verificationToken;
            try {
                verificationToken = emailVerificationService.createVerificationToken(newUser);
            } catch (Exception e) {
                System.err.println("Failed to create verification token for: " + newUser.getEmail());
                AuthResponse response = AuthResponse.error(
                        "Account created but verification email could not be prepared. Please try logging in.",
                        "TOKEN_CREATION_FAILED"
                );
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            boolean emailSent = emailService.sendVerificationEmail(newUser, verificationToken);

            if (emailSent) {
                System.out.println("Registration successful for: " + newUser.getName() +
                        " (ID: " + newUser.getId() + ") - Verification email sent");

                AuthResponse response = AuthResponse.success(
                        "Account created successfully! Please check your email and click the verification link to activate your account."
                );
                return ResponseEntity.status(HttpStatus.CREATED).body(response);

            } else {
                System.err.println("Registration completed but email sending failed for: " + newUser.getEmail());

                AuthResponse response = AuthResponse.error(
                        "Account created but verification email could not be sent. Please try logging in or contact support.",
                        "EMAIL_SEND_FAILED"
                );
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Registration validation error: " + e.getMessage());
            AuthResponse response = AuthResponse.error(e.getMessage(), "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            System.err.println("Registration error: " + e.getMessage());
            AuthResponse response = AuthResponse.error(
                    "An error occurred during registration. Please try again."
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        try {
            System.out.println("Email verification attempt with token: " +
                    (token != null ? token.substring(0, Math.min(8, token.length())) + "..." : "null"));

            EmailVerificationService.VerificationResult result = emailVerificationService.verifyEmail(token);

            if (result.isSuccess()) {
                try {
                    emailService.sendWelcomeEmail(result.getUser());
                } catch (Exception e) {
                    System.err.println("Failed to send welcome email: " + e.getMessage());
                }

                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", "/email/email-verified.html")
                        .build();
            } else {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", "/email/email-verification-failed.html?error=" +
                                URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8))
                        .build();
            }

        } catch (Exception e) {
            System.err.println("Email verification error: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/email/email-verification-failed.html?error=" +
                            URLEncoder.encode("An unexpected error occurred during verification. Please try again or contact support.", StandardCharsets.UTF_8))
                    .build();
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

    @PostMapping("/resend-verification")
    public ResponseEntity<AuthResponse> resendVerificationEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.trim().isEmpty()) {
                AuthResponse response = AuthResponse.error("Email is required", "VALIDATION_ERROR");
                return ResponseEntity.badRequest().body(response);
            }

            Optional<User> userOpt = userService.findUserByEmail(email.trim());
            if (userOpt.isEmpty()) {
                AuthResponse response = AuthResponse.success("If an account with this email exists, a verification email has been sent.");
                return ResponseEntity.ok(response);
            }

            User user = userOpt.get();

            if (user.getEmailVerified()) {
                AuthResponse response = AuthResponse.error("This email is already verified. You can log in normally.", "ALREADY_VERIFIED");
                return ResponseEntity.badRequest().body(response);
            }

            try {
                String token = emailVerificationService.resendVerificationEmail(user);
                boolean emailSent = emailService.sendVerificationEmail(user, token);

                if (emailSent) {
                    AuthResponse response = AuthResponse.success("Verification email sent! Please check your inbox.");
                    return ResponseEntity.ok(response);
                } else {
                    AuthResponse response = AuthResponse.error("Failed to send verification email. Please try again.", "EMAIL_SEND_FAILED");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            } catch (IllegalStateException e) {
                AuthResponse response = AuthResponse.error(e.getMessage(), "RATE_LIMITED");
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
            }

        } catch (Exception e) {
            System.err.println("Resend verification error: " + e.getMessage());
            AuthResponse response = AuthResponse.error("An error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/send-welcome-email")
    public ResponseEntity<Map<String, String>> sendWelcomeEmail(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user_id") == null) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Authentication required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Long userId = (Long) session.getAttribute("user_id");
            Optional<User> userOpt = userService.findUserById(userId);

            if (userOpt.isPresent()) {
                boolean sent = emailService.sendWelcomeEmail(userOpt.get());
                Map<String, String> response = new HashMap<>();
                response.put("message", sent ? "Welcome email sent" : "Failed to send welcome email");
                return ResponseEntity.ok(response);
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            System.err.println("Send welcome email error: " + e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("message", "An error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}