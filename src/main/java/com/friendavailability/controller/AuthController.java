package com.friendavailability.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/user")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal OAuth2User oAuth2User){
        Map<String, Object> userInfo = new HashMap<>();

        if(oAuth2User != null){
            userInfo.put("authenticated", true);
            userInfo.put("name", oAuth2User.getAttribute("name"));
            userInfo.put("email", oAuth2User.getAttribute("email"));
            userInfo.put("googleId", oAuth2User.getAttribute("sub"));
            userInfo.put("picture", oAuth2User.getAttribute("picture"));
        }else{
            userInfo.put("authenticated", false);
        }
        return userInfo;
    }

    @GetMapping("/status")
    public Map<String, Object> getAuthStatus(@AuthenticationPrincipal OAuth2User oAuth2User){
        Map<String, Object> status = new HashMap<>();
        status.put("loggedIn", oAuth2User != null);

        if(oAuth2User != null){
            status.put("name", oAuth2User.getAttribute("name"));
        }
        return status;
    }


}
