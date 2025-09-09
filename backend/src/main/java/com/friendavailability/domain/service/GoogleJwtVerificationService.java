package com.friendavailability.domain.service;

import com.friendavailability.domain.exception.GoogleAuthenticationException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
@Slf4j
public class GoogleJwtVerificationService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    private final GoogleIdTokenVerifier verifier;

    public GoogleJwtVerificationService() {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    public GoogleUserInfo verifyToken(String tokenString) {
        if (tokenString == null || tokenString.trim().isEmpty()) {
            throw GoogleAuthenticationException.missingCredential();
        }

        log.info("Verifying Google JWT token");

        try {
            GoogleIdToken idToken = verifier.verify(tokenString);

            if (idToken == null) {
                throw GoogleAuthenticationException.invalidToken();
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            // Verify audience (client ID)
            if (!payload.getAudience().equals(googleClientId)) {
                throw GoogleAuthenticationException.invalidAudience(googleClientId, payload.getAudience().toString());
            }

            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            Boolean emailVerified = payload.getEmailVerified();

            log.info("JWT verification successful for user: {}", email);

            return new GoogleUserInfo(googleId, email, name, emailVerified);

        } catch (GeneralSecurityException | IOException e) {
            throw GoogleAuthenticationException.tokenVerificationFailed(e.getMessage());
        }
    }

    @Getter
    public static class GoogleUserInfo {

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