package com.friendavailability.config;

import com.friendavailability.model.User;
import com.friendavailability.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("==========================================");
        System.out.println("CustomOAuth2UserService.loadUser() CALLED!");
        System.out.println("==========================================");

        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String googleId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        System.out.println("OAuth User Info:");
        System.out.println("  Name: " + name);
        System.out.println("  Email: " + email);
        System.out.println("  Google ID: " + googleId);

        // Validate that we have the required information
        if (email == null || googleId == null || name == null) {
            System.err.println("❌ Missing required OAuth information");
            throw new OAuth2AuthenticationException("Missing required user information from Google");
        }

        Optional<User> existingUser = userService.findUserByEmail(email);

        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            System.out.println("✅ Found existing user: " + user.getName() + " (ID: " + user.getId() + ")");

            // Check if we need to update the Google ID
            if (user.getGoogleId() == null || !user.getGoogleId().equals(googleId)) {
                System.out.println("🔗 Linking Google account to existing user...");

                try {
                    // Use the linkGoogleAccount method which handles the Google ID properly
                    Optional<User> linkedUser = userService.linkGoogleAccount(user.getId(), googleId);

                    if (linkedUser.isPresent()) {
                        user = linkedUser.get(); // Get the updated user
                        System.out.println("✅ Successfully linked Google account and verified email");
                    } else {
                        System.err.println("❌ Failed to link Google account");
                        throw new OAuth2AuthenticationException("Failed to link Google account");
                    }
                } catch (RuntimeException e) {
                    System.err.println("❌ Error linking Google account: " + e.getMessage());
                    // If Google ID already linked to another user, this is still an error we should handle
                    throw new OAuth2AuthenticationException("Google account linking failed: " + e.getMessage());
                }
            } else {
                System.out.println("✅ User already has correct Google ID");
            }
            System.out.println("🎉 Welcome back, " + user.getName() + "!");

        } else {
            System.out.println("👤 Creating new user for: " + name + " (" + email + ")");

            try {
                // Create the user first
                user = userService.createUser(name, email);
                System.out.println("✅ Created new user: " + user.getName() + " (ID: " + user.getId() + ")");

                // Then link the Google account
                Optional<User> linkedUser = userService.linkGoogleAccount(user.getId(), googleId);

                if (linkedUser.isPresent()) {
                    user = linkedUser.get(); // Get the updated user with Google ID
                    System.out.println("✅ Successfully linked Google account for new user");
                    System.out.println("🎉 Created new user account for " + name);
                } else {
                    System.err.println("❌ Failed to link Google account to new user");
                    // Clean up - delete the user we just created since linking failed
                    userService.deleteUserById(user.getId());
                    throw new OAuth2AuthenticationException("Failed to link Google account to new user");
                }

            } catch (RuntimeException e) {
                System.err.println("❌ Error during user creation: " + e.getMessage());
                e.printStackTrace();
                throw new OAuth2AuthenticationException("Failed to create user account: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("❌ Unexpected error during user creation: " + e.getMessage());
                e.printStackTrace();
                throw new OAuth2AuthenticationException("Unexpected error creating user account");
            }
        }

        System.out.println("==========================================");
        System.out.println("✅ OAuth user processing completed successfully");
        System.out.println("Final user state: " + user.getName() + " (ID: " + user.getId() + ", Google ID: " + user.getGoogleId() + ")");
        System.out.println("==========================================");

        return new DefaultOAuth2User(
                oauth2User.getAuthorities(),
                attributes,
                "sub"
        );
    }
}