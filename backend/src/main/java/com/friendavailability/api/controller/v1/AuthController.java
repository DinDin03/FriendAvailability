package com.friendavailability.api.controller.v1;

import com.friendavailability.api.dto.request.auth.AuthRequest;
import com.friendavailability.api.dto.response.auth.AuthResponse;
import com.friendavailability.api.dto.response.auth.UserDto;
import com.friendavailability.domain.entity.User;
import com.friendavailability.domain.service.*;

import lombok.extern.slf4j.Slf4j;
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
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:5173", "http://localhost:5174","http://127.0.0.1:8080", "https://friendavailability-production.up.railway.app", "https://www.linkups.com.au"})
@Slf4j
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;
    private final GoogleJwtVerificationService googleJwtVerificationService;


    public AuthController(UserService userService,
                          AuthService authService,
                          EmailVerificationService emailVerificationService,
                          EmailService emailService,
                          GoogleJwtVerificationService googleJwtVerificationService) {
        this.userService = userService;
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
        this.emailService = emailService;
        this.googleJwtVerificationService = googleJwtVerificationService;
    }
    @PostMapping("/google-signin")
    public ResponseEntity<UserDto> googleSignin(@RequestBody Map<String,String> request, HttpServletRequest httpRequest){
        log.info("Processing Google JWT signin request");

        String credential = request.get("credential");
        User authenticatedUser = authService.authenticateWithGoogleJwt(credential, httpRequest);
        UserDto userDto = UserDto.fromUser(authenticatedUser);

        log.info("Google JWT signin successful for user: {}", authenticatedUser.getId());
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest loginRequest,
                                              HttpServletRequest httpRequest) {

        log.info("Login attempt for email: {}", loginRequest.getEmail());

        User authenticatedUser = authService.authenticateAndLogin(loginRequest, httpRequest);
        UserDto userDto = UserDto.fromUser(authenticatedUser);
        AuthResponse response = AuthResponse.success("Login successful", userDto, "/dashboard");

        log.info("User {} logged in successfully", authenticatedUser.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest registerRequest,
                                                 HttpServletRequest httpRequest) {

        log.info("Registration attempt for email: {}", registerRequest.getEmail());

        User newUser = authService.registerUser(registerRequest);

        boolean emailSent = emailVerificationService.setupAndSendVerification(newUser);

        String message = emailSent
                ? "Account created successfully! Please check your email and click the verification link to activate your account."
                : "Account created but verification email could not be sent. Please try logging in or contact support.";

        AuthResponse response = AuthResponse.success(message);

        log.info("User {} registered successfully, email sent: {}", newUser.getId(), emailSent);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/current-user")
    public ResponseEntity<UserDto> getCurrentUser(HttpServletRequest request) {

        log.debug("Checking current user authentication");

        User currentUser = authService.getCurrentAuthenticatedUser(request);

        UserDto userDto = UserDto.fromUser(currentUser);

        log.debug("Current user found: {}", currentUser.getId());
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {

        log.info("Processing logout request");

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
            log.debug("Session invalidated");
        }

        SecurityContextHolder.clearContext();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");

        log.info("User logged out successfully");
        return ResponseEntity.ok(response);
    }
}
