package com.friendavailability.controller;

import com.friendavailability.dto.auth.AuthRequest;
import com.friendavailability.dto.auth.AuthResponse;
import com.friendavailability.dto.auth.UserDto;
import com.friendavailability.model.User;
import com.friendavailability.service.UserService;
import com.friendavailability.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

public class AuthController {

}
