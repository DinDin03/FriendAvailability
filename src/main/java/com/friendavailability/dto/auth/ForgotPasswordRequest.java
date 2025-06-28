package com.friendavailability.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForgotPasswordRequest {

    @Email(message = "please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

}
