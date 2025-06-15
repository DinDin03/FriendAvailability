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
public class CustomOAuth2UserService extends DefaultOAuth2UserService{

    @Autowired
    private UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException{
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String googleId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        Optional<User> existingUser = userService.findUserByEmail(email);

        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.getGoogleId() == null || !user.getGoogleId().equals(googleId)) {
                user.setGoogleId(googleId);
                System.out.println("Updated existing user's Google ID");
            }
            System.out.println("Welcome back, " + user.getName() + "!");
        } else {
            user = userService.createUser(name, email);
            user.setGoogleId(googleId);
            System.out.println("Created new user account for " + name);
        }
        return new DefaultOAuth2User(
                oauth2User.getAuthorities(),
                attributes,
                "sub"
        );
    }
}