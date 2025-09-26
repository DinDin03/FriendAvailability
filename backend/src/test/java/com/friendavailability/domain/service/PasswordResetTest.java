package com.friendavailability.domain.service;

import com.friendavailability.base.BaseUnitTest;
import com.friendavailability.domain.entity.User;
import com.friendavailability.domain.entity.token.PasswordResetToken;
import com.friendavailability.domain.exception.ResourceNotFoundException;
import com.friendavailability.domain.exception.ValidationException;
import com.friendavailability.domain.repository.PasswordResetTokenRepository;
import com.friendavailability.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PasswordResetService
 * 
 * This test class focuses on SECURITY-CRITICAL functionality used at enterprise companies:
 * - Email enumeration prevention (security by design)
 * - Rate limiting to prevent brute force attacks
 * - Token expiration and single-use enforcement
 * - Password strength validation
 * - Graceful error handling without information leakage
 * 
 * Learning Focus:
 * - Security testing patterns
 * - Rate limiting validation
 * - Token lifecycle management
 * - Password hashing verification
 * - Information disclosure prevention
 */
class PasswordResetServiceTest extends BaseUnitTest {

    // ============== MOCKED DEPENDENCIES ==============
    
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private EmailService emailService;

    // ============== SERVICE UNDER TEST ==============
    
    @InjectMocks
    private PasswordResetService passwordResetService;

    // ============== TOKEN CREATION TESTS ==============
    // These test the secure token generation process

    @Test
    void shouldCreateResetTokenSuccessfully() {
        // ============== GIVEN ==============
        User verifiedUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("Test User")
                .emailVerified(true) // Must be verified for password reset
                .build();
        
        String expectedToken = "unique-reset-token-uuid";
        
        // Mock rate limiting check (user is under limit)
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(verifiedUser), any(LocalDateTime.class)))
                .willReturn(2); // Under the limit of 3
        
        // Mock no existing valid token
        given(tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(verifiedUser), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        
        // Mock token uniqueness check
        given(tokenRepository.findByToken(anyString()))
                .willReturn(Optional.empty());
        
        // Mock token saving
        given(tokenRepository.save(any(PasswordResetToken.class)))
                .willAnswer(invocation -> {
                    PasswordResetToken token = invocation.getArgument(0);
                    return token.toBuilder().id(1L).build();
                });

        // ============== WHEN ==============
        String actualToken = passwordResetService.createResetToken(verifiedUser);

        // ============== THEN ==============
        assertThat(actualToken).isNotNull().isNotBlank();
        
        // Verify token properties
        ArgumentCaptor<PasswordResetToken> tokenCaptor = 
                ArgumentCaptor.forClass(PasswordResetToken.class);
        then(tokenRepository).should().save(tokenCaptor.capture());
        
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(verifiedUser);
        assertThat(savedToken.getToken()).isEqualTo(actualToken);
        assertThat(savedToken.isUsed()).isFalse();
        assertThat(savedToken.getCreatedAt()).isNotNull();
        assertThat(savedToken.getExpiresAt()).isNotNull();
        
        // Verify 30-minute expiry (security requirement)
        LocalDateTime expectedExpiry = savedToken.getCreatedAt().plusMinutes(30);
        assertThat(savedToken.getExpiresAt()).isEqualTo(expectedExpiry);
    }

    @Test
    void shouldThrowExceptionForNullUser() {
        // ============== GIVEN ==============
        User nullUser = null;

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> passwordResetService.createResetToken(nullUser))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User cannot be null");
        
        then(tokenRepository).should(never()).save(any());
    }

    @Test
    void shouldThrowExceptionForUnverifiedEmail() {
        // ============== GIVEN ==============
        User unverifiedUser = User.builder()
                .id(1L)
                .email("unverified@example.com")
                .emailVerified(false) // Not verified - security risk
                .build();

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> passwordResetService.createResetToken(unverifiedUser))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("unverified email");
        
        then(tokenRepository).should(never()).save(any());
    }

    // ============== RATE LIMITING TESTS (Security Critical) ==============
    
    @Test
    void shouldEnforceRateLimitingForTokenCreation() {
        // ============== GIVEN ==============
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .emailVerified(true)
                .build();
        
        // Mock rate limit exceeded (more than 3 attempts in 30 minutes)
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(user), any(LocalDateTime.class)))
                .willReturn(4); // Over the limit

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> passwordResetService.createResetToken(user))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("rate limit");
        
        // Verify no token was created due to rate limiting
        then(tokenRepository).should(never()).save(any());
    }

    @Test
    void shouldReturnExistingValidToken() {
        // ============== GIVEN ==============
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .emailVerified(true)
                .build();
        
        String existingToken = "existing-valid-token";
        
        PasswordResetToken validToken = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .token(existingToken)
                .used(false)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .build();
        
        // Mock rate limiting check
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(user), any(LocalDateTime.class)))
                .willReturn(2);
        
        // Mock existing valid token
        given(tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(user), any(LocalDateTime.class)))
                .willReturn(Optional.of(validToken));

        // ============== WHEN ==============
        String returnedToken = passwordResetService.createResetToken(user);

        // ============== THEN ==============
        assertThat(returnedToken).isEqualTo(existingToken);
        
        // Verify no new token was created
        then(tokenRepository).should(never()).save(any(PasswordResetToken.class));
    }

    // ============== PASSWORD RESET TESTS ==============
    // These test the actual password changing functionality

    @Test
    void shouldResetPasswordSuccessfully() {
        // ============== GIVEN ==============
        String newPassword = "newSecurePassword123";
        String hashedPassword = "hashed-new-password";
        String tokenValue = "valid-reset-token";
        
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("old-hashed-password")
                .build();
        
        PasswordResetToken validToken = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .token(tokenValue)
                .used(false)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .build();
        
        // Mock token lookup and validation
        given(tokenRepository.findByToken(tokenValue))
                .willReturn(Optional.of(validToken));
        
        // Mock password encoding
        given(passwordEncoder.encode(newPassword))
                .willReturn(hashedPassword);
        
        // Mock repository saves
        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(tokenRepository.save(any(PasswordResetToken.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // ============== WHEN ==============
        passwordResetService.resetPassword(tokenValue, newPassword);

        // ============== THEN ==============
        // Verify password was hashed and user updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo(hashedPassword);
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        
        // Verify token was marked as used
        ArgumentCaptor<PasswordResetToken> tokenCaptor = 
                ArgumentCaptor.forClass(PasswordResetToken.class);
        then(tokenRepository).should().save(tokenCaptor.capture());
        
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.isUsed()).isTrue();
        
        // Verify password encoding was called
        then(passwordEncoder).should().encode(newPassword);
    }

    @Test
    void shouldValidatePasswordStrength() {
        // Test short password
        String shortPassword = "short";
        String tokenValue = "valid-token";
        
        assertThatThrownBy(() -> passwordResetService.resetPassword(tokenValue, shortPassword))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("8 characters");
        
        // Test null password
        assertThatThrownBy(() -> passwordResetService.resetPassword(tokenValue, null))
                .isInstanceOf(ValidationException.class);
        
        // Test empty password
        assertThatThrownBy(() -> passwordResetService.resetPassword(tokenValue, ""))
                .isInstanceOf(ValidationException.class);
        
        // Verify no repository calls for invalid passwords
        then(tokenRepository).should(never()).findByToken(anyString());
    }

    @Test
    void shouldValidateTokenInput() {
        String validPassword = "validPassword123";
        
        // Test null token
        assertThatThrownBy(() -> passwordResetService.resetPassword(null, validPassword))
                .isInstanceOf(ValidationException.class);
        
        // Test empty token
        assertThatThrownBy(() -> passwordResetService.resetPassword("", validPassword))
                .isInstanceOf(ValidationException.class);
        
        // Test whitespace token
        assertThatThrownBy(() -> passwordResetService.resetPassword("   ", validPassword))
                .isInstanceOf(ValidationException.class);
    }

    // ============== TOKEN VALIDATION TESTS ==============
    
    @Test
    void shouldHandleNonExistentToken() {
        // ============== GIVEN ==============
        String nonExistentToken = "non-existent-token";
        
        given(tokenRepository.findByToken(nonExistentToken))
                .willReturn(Optional.empty());

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> passwordResetService.validateResetToken(nonExistentToken))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("token not found");
    }

    @Test
    void shouldHandleExpiredToken() {
        // ============== GIVEN ==============
        String expiredTokenValue = "expired-token";
        
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();
        
        // Create expired token (created 45 minutes ago, expires after 30 minutes)
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .token(expiredTokenValue)
                .used(false)
                .createdAt(LocalDateTime.now().minusMinutes(45))
                .expiresAt(LocalDateTime.now().minusMinutes(15)) // Expired 15 minutes ago
                .build();
        
        given(tokenRepository.findByToken(expiredTokenValue))
                .willReturn(Optional.of(expiredToken));

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> passwordResetService.validateResetToken(expiredTokenValue))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void shouldHandleAlreadyUsedToken() {
        // ============== GIVEN ==============
        String usedTokenValue = "used-token";
        
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();
        
        PasswordResetToken usedToken = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .token(usedTokenValue)
                .used(true) // Already used
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .build();
        
        given(tokenRepository.findByToken(usedTokenValue))
                .willReturn(Optional.of(usedToken));

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> passwordResetService.validateResetToken(usedTokenValue))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("used");
    }

    @Test
    void shouldValidateTokenSuccessfully() {
        // ============== GIVEN ==============
        String validTokenValue = "valid-token";
        
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("Test User")
                .build();
        
        PasswordResetToken validToken = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .token(validTokenValue)
                .used(false)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .build();
        
        given(tokenRepository.findByToken(validTokenValue))
                .willReturn(Optional.of(validToken));

        // ============== WHEN ==============
        User returnedUser = passwordResetService.validateResetToken(validTokenValue);

        // ============== THEN ==============
        assertThat(returnedUser).isEqualTo(user);
        
        then(tokenRepository).should().findByToken(validTokenValue);
    }

    // ============== EMAIL ENUMERATION PREVENTION TESTS (Critical Security) ==============
    // These test that we don't reveal whether emails exist in the system

    @Test
    void shouldNotRevealNonExistentEmailDuringReset() {
        // ============== GIVEN ==============
        String nonExistentEmail = "nonexistent@example.com";
        
        given(userRepository.findByEmail(nonExistentEmail))
                .willReturn(Optional.empty());

        // ============== WHEN ==============
        // This should NOT throw an exception (security by design)
        passwordResetService.requestPasswordReset(nonExistentEmail);

        // ============== THEN ==============
        // Verify email service was NOT called for non-existent user
        then(emailService).should(never()).sendPasswordResetEmail(any(), anyString());
        
        // Verify no token creation attempt
        then(tokenRepository).should(never()).save(any());
    }

    @Test
    void shouldNotRevealUnverifiedEmailDuringReset() {
        // ============== GIVEN ==============
        String unverifiedEmail = "unverified@example.com";
        
        User unverifiedUser = User.builder()
                .id(1L)
                .email(unverifiedEmail)
                .emailVerified(false) // Unverified email
                .build();
        
        given(userRepository.findByEmail(unverifiedEmail))
                .willReturn(Optional.of(unverifiedUser));

        // ============== WHEN ==============
        // Should NOT throw exception (security by design)
        passwordResetService.requestPasswordReset(unverifiedEmail);

        // ============== THEN ==============
        // Verify no email was sent to unverified user
        then(emailService).should(never()).sendPasswordResetEmail(any(), anyString());
        
        // Verify no token was created
        then(tokenRepository).should(never()).save(any());
    }

    @Test
    void shouldProcessValidEmailResetRequest() {
        // ============== GIVEN ==============
        String validEmail = "verified@example.com";
        
        User verifiedUser = User.builder()
                .id(1L)
                .email(validEmail)
                .emailVerified(true)
                .build();
        
        String generatedToken = "generated-reset-token";
        
        given(userRepository.findByEmail(validEmail))
                .willReturn(Optional.of(verifiedUser));
        
        // Mock token creation flow
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(verifiedUser), any(LocalDateTime.class)))
                .willReturn(0);
        given(tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(verifiedUser), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(tokenRepository.findByToken(anyString()))
                .willReturn(Optional.empty());
        given(tokenRepository.save(any(PasswordResetToken.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        
        // Mock successful email sending
        given(emailService.sendPasswordResetEmail(eq(verifiedUser), anyString()))
                .willReturn(true);

        // ============== WHEN ==============
        passwordResetService.requestPasswordReset(validEmail);

        // ============== THEN ==============
        // Verify token was created
        then(tokenRepository).should().save(any(PasswordResetToken.class));
        
        // Verify email was sent
        then(emailService).should().sendPasswordResetEmail(eq(verifiedUser), anyString());
    }

    @Test
    void shouldHandleEmailServiceFailureGracefully() {
        // ============== GIVEN ==============
        String email = "user@example.com";
        
        User user = User.builder()
                .id(1L)
                .email(email)
                .emailVerified(true)
                .build();
        
        given(userRepository.findByEmail(email))
                .willReturn(Optional.of(user));
        
        // Mock token creation
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(user), any(LocalDateTime.class)))
                .willReturn(0);
        given(tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(user), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(tokenRepository.findByToken(anyString()))
                .willReturn(Optional.empty());
        given(tokenRepository.save(any(PasswordResetToken.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        
        // Mock email service failure
        given(emailService.sendPasswordResetEmail(eq(user), anyString()))
                .willReturn(false);

        // ============== WHEN ==============
        // Should not throw exception even when email fails
        passwordResetService.requestPasswordReset(email);

        // ============== THEN ==============
        // Verify token was still created
        then(tokenRepository).should().save(any(PasswordResetToken.class));
        
        // Verify email service was attempted
        then(emailService).should().sendPasswordResetEmail(eq(user), anyString());
    }

    @Test
    void shouldHandleRateLimitExceptionDuringRequest() {
        // ============== GIVEN ==============
        String email = "ratelimited@example.com";
        
        User user = User.builder()
                .id(1L)
                .email(email)
                .emailVerified(true)
                .build();
        
        given(userRepository.findByEmail(email))
                .willReturn(Optional.of(user));
        
        // Mock rate limit exceeded
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(user), any(LocalDateTime.class)))
                .willReturn(4); // Over limit

        // ============== WHEN & THEN ==============
        // Rate limiting exceptions should be re-thrown (not hidden)
        assertThatThrownBy(() -> passwordResetService.requestPasswordReset(email))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("rate limit");
        
        // Verify no email was sent due to rate limiting
        then(emailService).should(never()).sendPasswordResetEmail(any(), anyString());
    }
}