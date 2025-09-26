package com.friendavailability.api.dto.response.auth;

import lombok.Data;

@Data
public class GoogleUserInfo {
    private String email;
    private String name;
    private String googleId;
    private String picture;
    private boolean emailVerified;
}