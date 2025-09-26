package com.friendavailability.api.dto.response.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleUserInfo {

        private final String googleId;
        private final String email;
        private final String name;
        private final boolean emailVerified;

        public GoogleUserInfo(String googleId, String email, String name, Boolean emailVerified) {
            this.googleId = googleId;
            this.email = email;
            this.name = name;
            this.emailVerified = emailVerified != null ? emailVerified : false;
        }

        @Override
        public String toString() {
            return String.format("GoogleUserInfo{googleId='%s', email='%s', name='%s', emailVerified=%s}",
                    googleId, email, name, emailVerified);
        }
}