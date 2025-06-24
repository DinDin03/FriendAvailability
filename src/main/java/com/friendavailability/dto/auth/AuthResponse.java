package com.friendavailability.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private boolean success;
    private String message;
    private UserDto userDto;
    private String redirectUrl;
    private String errorCode;

    public static AuthResponse success(String message, UserDto userDto, String redirectUrl){
        return AuthResponse.builder()
                .success(true)
                .message(message)
                .userDto(userDto)
                .redirectUrl(redirectUrl)
                .build();
    }

    public static AuthResponse success(String message, String redirectUrl) {
        return AuthResponse.builder()
                .success(true)
                .message(message)
                .redirectUrl(redirectUrl)
                .build();
    }

    public static AuthResponse success(String message) {
        return AuthResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .success(false)
                .message(message)
                .build();
    }

    public static AuthResponse error(String message, String errorCode) {
        return AuthResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
