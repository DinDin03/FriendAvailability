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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest loginRequest, HttpServletRequest httpRequest){
        try{
            Optional<User> authenticatedUser = authService.authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());
            if(authenticatedUser.isPresent()) {
                User user = authenticatedUser.get();

                HttpSession session = httpRequest.getSession(true);
                session.setAttribute("user_id", user.getId());
                session.setAttribute("user_email", user.getEmail());
                session.setAttribute("authenticated", true);

                System.out.println("Login successful for " + loginRequest.getName());

                AuthResponse response = AuthResponse.success(
                        "Welcome back! Login succesful",
                        UserDto.fromUser(user),
                        "/dashboard.html"
                );
                return ResponseEntity.ok(response);
            }
            else {
                System.out.println("Authenticate failed for " + loginRequest.getName());
                AuthResponse response = AuthResponse.error(
                        "Invalid email or password. Please try again.",
                        "INVALID_CREDENTIALS"
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        }catch(Exception e){
            AuthResponse response = AuthResponse.error("An error occurred during login. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest registerRequest, HttpServletRequest httpRequest){
        try {
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
    public ResponseEntity<UserDto> getCurrentUser(HttpServletRequest request, HttpSession httpSession){
        try{
            HttpSession session = request.getSession(false);
            if(session != null && session.getAttribute("authenticated") != null){
                Long userId = (Long) session.getAttribute("user_id");
                if(userId != null){
                    Optional<User> user = userService.findUserById(userId);
                    if(user.isPresent()){
                        return ResponseEntity.ok(UserDto.fromUser(user.get()));
                    }
                }
            }


        }
    }

}
