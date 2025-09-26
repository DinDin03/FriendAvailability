package com.friendavailability.domain.service;

import com.friendavailability.base.BaseUnitTest;
import com.friendavailability.api.dto.request.auth.AuthRequest;
import com.friendavailability.domain.entity.User;
import com.friendavailability.domain.exception.ResourceNotFoundException;
import com.friendavailability.domain.exception.ValidationException;
import com.friendavailability.domain.repository.UserRepository;
import com.friendavailability.api.dto.response.auth.GoogleUserInfo;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

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
                .password(encodedPassword)
                .isActive(true)
                .emailVerified(true)
                .build();
        
        given(userService.findUserByEmail(email)).willReturn(user);
        given(passwordEncoder.matches(password, encodedPassword)).willReturn(true);
        given(httpRequest.getSession(true)).willReturn(httpSession);

        // When
        User authenticatedUser = authService.authenticateAndLogin(loginRequest, httpRequest);

        // Then
        assertThat(authenticatedUser).isEqualTo(user);
        then(userService).should().findUserByEmail(email);
        then(passwordEncoder).should().matches(password, encodedPassword);
        then(httpSession).should().setAttribute("user", user);
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
                .password(encodedPassword)
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
                .isActive(false)
                .emailVerified(true)
                .build();
        
        given(userService.findUserByEmail(email)).willReturn(user);

        // When & Then
        assertThatThrownBy(() -> authService.authenticateAndLogin(loginRequest, httpRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Account is deactivated");
        
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
    void shouldProcessGoogleJwtTokenSuccessfully() throws GeneralSecurityException, IOException {
        // Given
        String jwtToken = "valid.jwt.token";
        String email = "google.user@example.com";
        String name = "Google User";
        String googleId = "google123";
        
        GoogleUserInfo googleUserInfo = new GoogleUserInfo();
        googleUserInfo.setEmail(email);
        googleUserInfo.setName(name);
        googleUserInfo.setGoogleId(googleId);
        
        User existingUser = User.builder()
                .id(1L)
                .email(email)
                .name(name)
                .googleId(googleId)
                .emailVerified(true)
                .isActive(true)
                .build();
        
        given(googleJwtVerificationService.verifyToken(jwtToken)).willReturn(googleUserInfo);
        given(userRepository.findByGoogleId(googleId)).willReturn(Optional.of(existingUser));
        given(httpRequest.getSession(true)).willReturn(httpSession);

        // When
        User authenticatedUser = authService.processGoogleJwtToken(jwtToken, httpRequest);

        // Then
        assertThat(authenticatedUser).isEqualTo(existingUser);
        then(googleJwtVerificationService).should().verifyToken(jwtToken);
        then(userRepository).should().findByGoogleId(googleId);
        then(httpSession).should().setAttribute("user", existingUser);
    }

    @Test
    void shouldCreateNewUserForFirstTimeGoogleLogin() throws GeneralSecurityException, IOException {
        // Given
        String jwtToken = "valid.jwt.token";
        String email = "new.google.user@example.com";
        String name = "New Google User";
        String googleId = "newGoogle123";
        
        GoogleUserInfo googleUserInfo = new GoogleUserInfo();
        googleUserInfo.setEmail(email);
        googleUserInfo.setName(name);
        googleUserInfo.setGoogleId(googleId);
        
        User newUser = User.builder()
                .id(2L)
                .email(email)
                .name(name)
                .googleId(googleId)
                .emailVerified(true)
                .isActive(true)
                .build();
        
        given(googleJwtVerificationService.verifyToken(jwtToken)).willReturn(googleUserInfo);
        given(userRepository.findByGoogleId(googleId)).willReturn(Optional.empty());
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());
        given(userService.createUserFromGoogle(googleUserInfo)).willReturn(newUser);
        given(httpRequest.getSession(true)).willReturn(httpSession);

        // When
        User authenticatedUser = authService.processGoogleJwtToken(jwtToken, httpRequest);

        // Then
        assertThat(authenticatedUser).isEqualTo(newUser);
        then(googleJwtVerificationService).should().verifyToken(jwtToken);
        then(userRepository).should().findByGoogleId(googleId);
        then(userRepository).should().findByEmail(email);
        then(userService).should().createUserFromGoogle(googleUserInfo);
        then(httpSession).should().setAttribute("user", newUser);
    }

    @Test
    void shouldThrowValidationExceptionForInvalidGoogleJwt() throws GeneralSecurityException, IOException {
        // Given
        String invalidJwtToken = "invalid.jwt.token";
        
        given(googleJwtVerificationService.verifyToken(invalidJwtToken))
                .willThrow(new GeneralSecurityException("Invalid token"));

        // When & Then
        assertThatThrownBy(() -> authService.processGoogleJwtToken(invalidJwtToken, httpRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid Google JWT token");
        
        then(googleJwtVerificationService).should().verifyToken(invalidJwtToken);
        then(userRepository).should(never()).findByGoogleId(anyString());
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        String email = "newuser@example.com";
        String password = "securePassword";
        String name = "New User";
        String encodedPassword = "encodedSecurePassword";
        
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);
        registerRequest.setName(name);
        
        User newUser = User.builder()
                .id(1L)
                .email(email)
                .name(name)
                .password(encodedPassword)
                .isActive(true)
                .emailVerified(false)
                .build();
        
        given(userService.createUser(email, name, password)).willReturn(newUser);

        // When
        User registeredUser = authService.registerUser(registerRequest);

        // Then
        assertThat(registeredUser).isEqualTo(newUser);
        then(userService).should().createUser(email, name, password);
    }

    @Test
    void shouldGetCurrentUserFromSession() {
        // Given
        User sessionUser = User.builder()
                .id(1L)
                .email("session@example.com")
                .name("Session User")
                .build();
        
        given(httpRequest.getSession(false)).willReturn(httpSession);
        given(httpSession.getAttribute("user")).willReturn(sessionUser);

        // When
        User currentUser = authService.getCurrentUser(httpRequest);

        // Then
        assertThat(currentUser).isEqualTo(sessionUser);
        then(httpRequest).should().getSession(false);
        then(httpSession).should().getAttribute("user");
    }

    @Test
    void shouldReturnNullWhenNoSessionExists() {
        // Given
        given(httpRequest.getSession(false)).willReturn(null);

        // When
        User currentUser = authService.getCurrentUser(httpRequest);

        // Then
        assertThat(currentUser).isNull();
        then(httpRequest).should().getSession(false);
    }

    @Test
    void shouldReturnNullWhenNoUserInSession() {
        // Given
        given(httpRequest.getSession(false)).willReturn(httpSession);
        given(httpSession.getAttribute("user")).willReturn(null);

        // When
        User currentUser = authService.getCurrentUser(httpRequest);

        // Then
        assertThat(currentUser).isNull();
        then(httpRequest).should().getSession(false);
        then(httpSession).should().getAttribute("user");
    }

    @Test
    void shouldLogoutUserSuccessfully() {
        // Given
        given(httpRequest.getSession(false)).willReturn(httpSession);

        // When
        authService.logout(httpRequest);

        // Then
        then(httpRequest).should().getSession(false);
        then(httpSession).should().invalidate();
    }

    @Test
    void shouldHandleLogoutWhenNoSessionExists() {
        // Given
        given(httpRequest.getSession(false)).willReturn(null);

        // When & Then (should not throw exception)
        authService.logout(httpRequest);
        
        then(httpRequest).should().getSession(false);
    }
}