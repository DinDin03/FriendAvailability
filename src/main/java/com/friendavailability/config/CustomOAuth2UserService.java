package com.friendavailability.config;

import com.friendavailability.model.User;
import com.friendavailability.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    @Autowired
    private UserService userService;

    private final OidcUserService delegate = new OidcUserService();

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        OidcUser oidcUser = delegate.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();

        String googleId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        System.out.println("OAuth User Info:");
        System.out.println("  Name: " + name);
        System.out.println("  Email: " + email);
        System.out.println("  Google ID: " + googleId);

        if (email == null || googleId == null || name == null) {
            System.err.println("Missing required OAuth information");
            throw new OAuth2AuthenticationException("Missing required user information from Google");
        }

        try {
            Optional<User> userByGoogleId = userService.findUserByGoogleId(googleId);

            if (userByGoogleId.isPresent()) {
                User user = userByGoogleId.get();
                System.out.println("Found existing user by Google ID: " + user.getName() + " (ID: " + user.getId() + ")");
                System.out.println("Welcome back, " + user.getName() + "!");

                return oidcUser;
            }

            Optional<User> userByEmail = userService.findUserByEmail(email);

            if (userByEmail.isPresent()) {
                User user = userByEmail.get();
                System.out.println("Found existing user by email: " + user.getName() + " (ID: " + user.getId() + ")");

                if (user.getGoogleId() == null) {
                    System.out.println("Linking Google account to existing user...");
                    Optional<User> linkedUser = userService.linkGoogleAccount(user.getId(), googleId);

                    if (linkedUser.isPresent()) {
                        user = linkedUser.get();
                        System.out.println("Successfully linked Google account");
                    } else {
                        throw new OAuth2AuthenticationException("Failed to link Google account");
                    }
                }

                System.out.println("Welcome back, " + user.getName() + "!");

                return oidcUser;
            }

            System.out.println("Creating new user for: " + name + " (" + email + ")");

            User newUser = userService.createUserWithGoogle(name, email, googleId);
            System.out.println("Created new user: " + newUser.getName() + " (ID: " + newUser.getId() + ")");
            System.out.println("Welcome, " + newUser.getName() + "!");

            return oidcUser;

        } catch (RuntimeException e) {
            System.err.println("Error during OAuth user processing: " + e.getMessage());
            e.printStackTrace();
            throw new OAuth2AuthenticationException("Failed to process OAuth user: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during OAuth processing: " + e.getMessage());
            e.printStackTrace();
            throw new OAuth2AuthenticationException("Unexpected error during authentication");
        }
    }
}