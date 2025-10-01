package com.linkups.domain.service;

import com.linkups.base.BaseUnitTest;
import com.linkups.api.dto.request.auth.AuthRequest;
import com.linkups.domain.entity.User;
import com.linkups.domain.exception.ResourceNotFoundException;
import com.linkups.domain.exception.ValidationException;
import com.linkups.domain.repository.UserRepository;
import com.linkups.api.dto.response.auth.GoogleUserInfo;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

//all tests passed
class AuthServiceTest extends BaseUnitTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private GoogleJwtVerificationService googleJwtVerificationService;
    
    @Mock
    private HttpServletRequest httpRequest;
    
    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldAuthenticateUserWithValidCredentials() {
        // Given
        String email = "test@example.com";
        String password = "validPassword";
        String encodedPassword = "encodedPassword";
        
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);
        
        User user = User.builder()
                .id(1L)
                .email(email)
                .name("Test User")
                .passwordHash(encodedPassword)
                .isActive(true)
                .emailVerified(true)
                .build();
        
        given(userService.findUserByEmail(email)).willReturn(user);
        given(passwordEncoder.matches(password, encodedPassword)).willReturn(true);
        given(httpRequest.getSession()).willReturn(httpSession);

        // When
        User authenticatedUser = authService.authenticateAndLogin(loginRequest, httpRequest);

        // Then
        assertThat(authenticatedUser).isEqualTo(user);
        then(userService).should().findUserByEmail(email);
        then(passwordEncoder).should().matches(password, encodedPassword);
        then(httpSession).should().setAttribute("authenticated", true);
    }

    @Test
    void shouldThrowValidationExceptionForInvalidPassword() {
        // Given
        String email = "test@example.com";
        String password = "wrongPassword";
        String encodedPassword = "encodedPassword";
        
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);
        
        User user = User.builder()
                .id(1L)
                .email(email)
                .passwordHash(encodedPassword)
                .isActive(true)
                .emailVerified(true)
                .build();
        
        given(userService.findUserByEmail(email)).willReturn(user);
        given(passwordEncoder.matches(password, encodedPassword)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.authenticateAndLogin(loginRequest, httpRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid email or password");
        
        then(userService).should().findUserByEmail(email);
        then(passwordEncoder).should().matches(password, encodedPassword);
        then(httpRequest).should(never()).getSession(anyBoolean());
    }

    @Test
    void shouldThrowValidationExceptionForUnverifiedEmail() {
        // Given
        String email = "test@example.com";
        String password = "validPassword";
        
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);
        
        User user = User.builder()
                .id(1L)
                .email(email)
                .passwordHash("hashedPassword")
                .isActive(true)
                .emailVerified(false)
                .build();
        
        given(userService.findUserByEmail(email)).willReturn(user);

        // When & Then
        assertThatThrownBy(() -> authService.authenticateAndLogin(loginRequest, httpRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Please verify your email before logging in");
        
        then(userService).should().findUserByEmail(email);
        then(passwordEncoder).should(never()).matches(anyString(), anyString());
    }

    @Test
    void shouldThrowValidationExceptionForInactiveUser() {
        // Given
        String email = "test@example.com";
        String password = "validPassword";
        
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);
        
        User user = User.builder()
                .id(1L)
                .email(email)
                .passwordHash("hashedPassword")
                .isActive(false)
                .emailVerified(true)
                .build();
        
        given(userService.findUserByEmail(email)).willReturn(user);

        // When & Then
        assertThatThrownBy(() -> authService.authenticateAndLogin(loginRequest, httpRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Your account has been disabled");
        
        then(userService).should().findUserByEmail(email);
        then(passwordEncoder).should(never()).matches(anyString(), anyString());
    }

    @Test
    void shouldThrowValidationExceptionForEmptyEmail() {
        // Given
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail("");
        loginRequest.setPassword("password");

        // When & Then
        assertThatThrownBy(() -> authService.authenticateAndLogin(loginRequest, httpRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email is required");
        
        then(userService).should(never()).findUserByEmail(anyString());
    }

    @Test
    void shouldThrowValidationExceptionForEmptyPassword() {
        // Given
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("");

        // When & Then
        assertThatThrownBy(() -> authService.authenticateAndLogin(loginRequest, httpRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password is required");
        
        then(userService).should(never()).findUserByEmail(anyString());
    }

    @Test
    void shouldProcessGoogleJwtTokenSuccessfully() {
        // Given
        String jwtToken = "valid.jwt.token";
        String email = "google.user@example.com";
        String name = "Google User";
        String googleId = "google123";
        
        GoogleUserInfo googleUserInfo = new GoogleUserInfo(googleId, email, name, true);

        User existingUser = User.builder()
                .id(1L)
                .email(email)
                .name(name)
                .googleId(googleId)
                .emailVerified(true)
                .isActive(true)
                .build();
        
        given(googleJwtVerificationService.verifyToken(jwtToken)).willReturn(googleUserInfo);
        given(userService.findUserByGoogleIdOptional(googleId)).willReturn(Optional.of(existingUser));
        given(httpRequest.getSession(true)).willReturn(httpSession);

        // When
        User authenticatedUser = authService.authenticateWithGoogleJwt(jwtToken, httpRequest);

        // Then
        assertThat(authenticatedUser).isEqualTo(existingUser);
        then(googleJwtVerificationService).should().verifyToken(jwtToken);
        then(userService).should().findUserByGoogleIdOptional(googleId);
        then(httpSession).should().setAttribute("authenticated", true);
    }

    @Test
    void shouldCreateNewUserForFirstTimeGoogleLogin() {
        // Given
        String jwtToken = "valid.jwt.token";
        String email = "new.google.user@example.com";
        String name = "New Google User";
        String googleId = "newGoogle123";
        
        GoogleUserInfo googleUserInfo = new GoogleUserInfo(googleId, email, name, true);

        User newUser = User.builder()
                .id(2L)
                .email(email)
                .name(name)
                .googleId(googleId)
                .emailVerified(true)
                .isActive(true)
                .build();
        
        given(googleJwtVerificationService.verifyToken(jwtToken)).willReturn(googleUserInfo);
        given(userService.findUserByGoogleIdOptional(googleId)).willReturn(Optional.empty());
        given(userService.findUserByEmailOptional(email)).willReturn(Optional.empty());
        given(userService.createUserWithGoogle(name, email, googleId)).willReturn(newUser);
        given(httpRequest.getSession(true)).willReturn(httpSession);

        // When
        User authenticatedUser = authService.authenticateWithGoogleJwt(jwtToken, httpRequest);

        // Then
        assertThat(authenticatedUser).isEqualTo(newUser);
        then(googleJwtVerificationService).should().verifyToken(jwtToken);
        then(userService).should().findUserByGoogleIdOptional(googleId);
        then(userService).should().findUserByEmailOptional(email);
        then(userService).should().createUserWithGoogle(name, email, googleId);
        then(httpSession).should().setAttribute("authenticated", true);
    }

    @Test
    void shouldThrowValidationExceptionForInvalidGoogleJwt() {
        // Given
        String invalidJwtToken = "invalid.jwt.token";
        
        given(googleJwtVerificationService.verifyToken(invalidJwtToken))
                .willThrow(new RuntimeException("Invalid token"));

        // When & Then
        assertThatThrownBy(() -> authService.authenticateWithGoogleJwt(invalidJwtToken, httpRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid token");
        
        then(googleJwtVerificationService).should().verifyToken(invalidJwtToken);
        then(userService).should(never()).findUserByGoogleId(anyString());
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        String email = "newuser@example.com";
        String password = "securePassword";
        String name = "New User";

        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);
        registerRequest.setName(name);

        User newUser = User.builder()
                .id(1L)
                .email(email.toLowerCase())
                .name(name.trim())
                .passwordHash("hashedPassword")
                .isActive(true)
                .emailVerified(false)
                .build();

        given(userRepository.existsByEmail(email.toLowerCase())).willReturn(false);
        given(passwordEncoder.encode(password)).willReturn("hashedPassword");
        given(userRepository.save(any(User.class))).willReturn(newUser);

        // When
        User registeredUser = authService.registerUser(registerRequest);

        // Then
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getEmail()).isEqualTo(email.toLowerCase());
        assertThat(registeredUser.getName()).isEqualTo(name.trim());
        then(userRepository).should().existsByEmail(email.toLowerCase());
        then(passwordEncoder).should().encode(password);
        then(userRepository).should().save(any(User.class));
    }

    @Test
    void shouldGetCurrentUserFromSession() {
        // Given
        Long userId = 1L;
        User sessionUser = User.builder()
                .id(userId)
                .email("session@example.com")
                .name("Session User")
                .build();

        given(httpRequest.getSession(false)).willReturn(httpSession);
        given(httpSession.getAttribute("authenticated")).willReturn(true);
        given(httpSession.getAttribute("user_id")).willReturn(userId);
        given(userService.findUserById(userId)).willReturn(sessionUser);

        // When
        User currentUser = authService.getCurrentAuthenticatedUser(httpRequest);

        // Then
        assertThat(currentUser).isEqualTo(sessionUser);
        then(httpRequest).should().getSession(false);
        then(httpSession).should().getAttribute("authenticated");
        then(httpSession).should().getAttribute("user_id");
        then(userService).should().findUserById(userId);
    }

    @Test
    void shouldThrowExceptionWhenNoSessionExists() {
        // Given
        given(httpRequest.getSession(false)).willReturn(null);

        // When & Then
        assertThatThrownBy(() -> authService.getCurrentAuthenticatedUser(httpRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No authenticated user found");

        then(httpRequest).should().getSession(false);
    }

    @Test
    void shouldThrowExceptionWhenNoUserInSession() {
        // Given
        given(httpRequest.getSession(false)).willReturn(httpSession);
        given(httpSession.getAttribute("authenticated")).willReturn(null);

        // When & Then
        assertThatThrownBy(() -> authService.getCurrentAuthenticatedUser(httpRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No authenticated user found");

        then(httpRequest).should().getSession(false);
        then(httpSession).should().getAttribute("authenticated");
    }

}