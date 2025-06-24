package com.friendavailability.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    private String password;

    @Override
    public String toString() {
        return "CreateUserRequest{name='" + name + "', email='" + email + "', hasPassword=" + (password != null) + "}";
    }
}