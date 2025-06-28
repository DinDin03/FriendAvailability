package com.friendavailability.controller;

import com.friendavailability.dto.auth.AuthResponse;
import com.friendavailability.dto.auth.ForgotPasswordRequest;
import com.friendavailability.dto.auth.ResetPasswordRequest;
import com.friendavailability.model.User;
import com.friendavailability.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:8080", "http://127.0.0.1:8080",
        "https://friendavailability-production.up.railway.app",
        "https://www.dineth.au"})
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService){
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request){
        try{
            boolean emailSent = passwordResetService.requestPasswordReset(request.getEmail());

            AuthResponse response = AuthResponse.success("If this email exists, we've sent a password reset link. Please check your inbox."
            );
            return ResponseEntity.ok(response);
        } catch(Exception e){
            AuthResponse response = AuthResponse.error("An error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/reset-password/validate")
    public ResponseEntity<AuthResponse> validateResetToken(@RequestParam String token){
        try{
            Optional<User> userOpt = passwordResetService.validateResetToken(token);
            if(userOpt.isPresent()){
                User user = userOpt.get();
                AuthResponse response = AuthResponse.success(
                        "Token is valid. You can now reset your password."
                );
                return ResponseEntity.ok(response);
            }else{
                AuthResponse response = AuthResponse.error(
                        "This password reset link is invalid or has expired. Please request a new one."
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }catch (Exception e) {

            AuthResponse response = AuthResponse.error("An error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        try {
            boolean success = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            if(success){
                AuthResponse response = AuthResponse.success(
                        "Password reset successful! You can now log in with your new password."
                );
                return ResponseEntity.ok(response);

            } else {
                AuthResponse response = AuthResponse.error(
                        "Password reset failed. The link may be invalid or expired. Please request a new reset link."
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }catch (IllegalArgumentException e) {
            AuthResponse response = AuthResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            System.err.println("Error resetting password: " + e.getMessage());

            AuthResponse response = AuthResponse.error("An error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


}
