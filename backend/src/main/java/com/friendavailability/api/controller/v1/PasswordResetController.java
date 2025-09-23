package com.friendavailability.api.controller.v1;

import com.friendavailability.api.dto.response.auth.AuthResponse;
import com.friendavailability.api.dto.request.auth.ForgotPasswordRequest;
import com.friendavailability.api.dto.request.auth.ResetPasswordRequest;
import com.friendavailability.domain.entity.User;
import com.friendavailability.domain.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:8080", "http://127.0.0.1:8080",
        "https://friendavailability-production.up.railway.app"})
@Slf4j
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
        log.info("PasswordResetController initialized successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Password reset requested for email: {}", request.getEmail());

        passwordResetService.requestPasswordReset(request.getEmail());

        AuthResponse response = AuthResponse.success(
                "If this email exists, we've sent a password reset link. Please check your inbox."
        );

        log.info("Password reset request processed for email: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reset-password/validate")
    public ResponseEntity<AuthResponse> validateResetToken(@RequestParam String token) {
        log.info("Validating password reset token: {}", token != null ? token.substring(0, 8) + "..." : "null");

        User user = passwordResetService.validateResetToken(token);

        AuthResponse response = AuthResponse.success(
                "Token is valid. You can now reset your password."
        );

        log.info("Password reset token validated successfully for user: {}", user.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Processing password reset for token: {}", request.getToken() != null ? request.getToken().substring(0, 8) + "..." : "null");

        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());

        AuthResponse response = AuthResponse.success(
                "Password reset successful! You can now log in with your new password."
        );

        log.info("Password reset completed successfully");
        return ResponseEntity.ok(response);
    }
}