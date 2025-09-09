package com.friendavailability.domain.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleJwtVerificationService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    private final GoogleIdTokenVerifier verifier;

    public GoogleJwtVerificationService() {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .build();
    }

    public GoogleUserInfo verifyToken(String tokenString) throws GeneralSecurityException, IOException {
        System.out.println("Verifying Google JWT token");

        GoogleIdToken idToken = verifier.verify(tokenString);

        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();

            // Verify the client id
            if (!payload.getAudience().equals(googleClientId)) {
                throw new IllegalArgumentException("Invalid token audience");
            }

            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            Boolean emailVerified = payload.getEmailVerified();

            System.out.println("JWT verification successful for user: " + email);

            return new GoogleUserInfo(googleId, email, name, emailVerified);

        } else {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    @Getter
    public static class GoogleUserInfo {
        // Getters
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
}