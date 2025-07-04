
package com.friendavailability.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    public AuthRequest(String email, String password){
        this.email = email;
        this.password = password;
    }

    public boolean isRegistration() {
        return name != null && !name.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "AuthRequest{" +
                "email='" + email + '\'' +
                ", hasPassword=" + (password != null && !password.isEmpty()) +
                ", name='" + name + '\'' +
                ", isRegistration=" + isRegistration() +
                '}';
    }
}
