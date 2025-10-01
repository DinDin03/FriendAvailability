package com.linkups.domain.service;

import com.linkups.base.BaseUnitTest;
import com.linkups.domain.entity.User;
import com.linkups.domain.entity.token.EmailVerificationToken;
import com.linkups.domain.repository.EmailVerificationTokenRepository;
import com.linkups.domain.repository.UserRepository;
import com.linkups.domain.service.EmailVerificationService.VerificationResult;
import com.linkups.domain.service.EmailVerificationService.VerificationStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailVerificationService
 * 
 * This test class demonstrates professional testing patterns used at enterprise companies:
 * - Complete isolation using mocks (@Mock annotations)
 * - Given-When-Then test structure for clarity
 * - BDDMockito for readable test syntax
 * - ArgumentCaptor for verifying method arguments
 * - Comprehensive edge case testing
 * 
 * Learning Focus:
 * - Mocking dependencies (repositories, services)
 * - Testing time-based business logic (rate limiting, expiration)
 * - Exception scenario testing
 * - Argument verification with Mockito
 */
//all tests passing
class EmailVerificationServiceTest extends BaseUnitTest {

    // ============== MOCKED DEPENDENCIES ==============
    // These represent external dependencies that we "fake" during testing
    
    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;

    // ============== SERVICE UNDER TEST ==============
    // @InjectMocks automatically injects the mocked dependencies above
    
    @InjectMocks
    private EmailVerificationService emailVerificationService;

    // ============== HAPPY PATH TESTS ==============
    // These test the main functionality when everything works correctly

    @Test
    void shouldSetupAndSendVerificationSuccessfully() {
        // ============== GIVEN (Test Setup) ==============
        // Create a test user who needs email verification
        User unverifiedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .emailVerified(false)  // Important: user is NOT verified yet
                .build();
        
        // Mock the token creation behavior
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(unverifiedUser), any(LocalDateTime.class)))
                .willReturn(0); // No recent token requests (under rate limit)
        
        given(tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(unverifiedUser), any(LocalDateTime.class)))
                .willReturn(Optional.empty()); // No existing valid token
        
        given(tokenRepository.findByToken(anyString()))
                .willReturn(Optional.empty()); // Token is unique
        
        given(tokenRepository.save(any(EmailVerificationToken.class)))
                .willAnswer(invocation -> {
                    EmailVerificationToken token = invocation.getArgument(0);
                    return EmailVerificationToken.builder()
                            .id(1L)
                            .user(token.getUser())
                            .token(token.getToken())
                            .expiresAt(token.getExpiresAt())
                            .createdAt(token.getCreatedAt())
                            .used(token.isUsed())
                            .build(); // Simulate database save
                });
        
        // Mock successful email sending
        given(emailService.sendVerificationEmail(eq(unverifiedUser), anyString()))
                .willReturn(true);

        // ============== WHEN (Action) ==============
        // Call the method we're testing
        boolean result = emailVerificationService.setupAndSendVerification(unverifiedUser);

        // ============== THEN (Verification) ==============
        // Verify the expected behavior occurred
        assertThat(result).isTrue();
        
        // Verify repository interactions
        then(tokenRepository).should().countByUserAndCreatedAtAfter(eq(unverifiedUser), any(LocalDateTime.class));
        then(tokenRepository).should().save(any(EmailVerificationToken.class));
        
        // Verify email service was called
        then(emailService).should().sendVerificationEmail(eq(unverifiedUser), anyString());
    }

    // ============== BUSINESS LOGIC TESTS ==============
    // These test specific business rules and constraints

    @Test
    void shouldThrowExceptionWhenUserIsNull() {
        // ============== GIVEN ==============
        User nullUser = null;

        // ============== WHEN & THEN ==============
        // Verify that the appropriate exception is thrown
        assertThatThrownBy(() -> emailVerificationService.createVerificationToken(nullUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User cannot be null");
        
        // Verify no repository calls were made
        then(tokenRepository).should(never()).save(any());
        then(emailService).should(never()).sendVerificationEmail(any(), anyString());
    }

    @Test
    void shouldThrowExceptionWhenUserAlreadyVerified() {
        // ============== GIVEN ==============
        User verifiedUser = User.builder()
                .id(1L)
                .email("verified@example.com")
                .name("Verified User")
                .emailVerified(true)  // Already verified!
                .build();

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> emailVerificationService.createVerificationToken(verifiedUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User email is already verified");
        
        // Verify no repository interactions occurred
        then(tokenRepository).should(never()).save(any());
    }

    @Test
    void shouldEnforceRateLimitingForTokenCreation() {
        // ============== GIVEN ==============
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .emailVerified(false)
                .build();
        
        // Mock rate limiting: user has already requested 3 tokens in the last hour
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(user), any(LocalDateTime.class)))
                .willReturn(3); // At the rate limit threshold

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> emailVerificationService.createVerificationToken(user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Too many verification email requests. Please wait before requesting another email.");
        
        // Verify no token was saved due to rate limiting
        then(tokenRepository).should(never()).save(any());
    }

    // ============== ARGUMENT CAPTURE TEST ==============
    // This demonstrates how to verify the exact arguments passed to mocked methods

    @Test
    void shouldCreateTokenWithCorrectProperties() {
        // ============== GIVEN ==============
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .emailVerified(false)
                .build();
        
        // Set up mocks
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(user), any(LocalDateTime.class)))
                .willReturn(0);
        given(tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(user), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(tokenRepository.findByToken(anyString()))
                .willReturn(Optional.empty());
        given(tokenRepository.save(any(EmailVerificationToken.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // ============== WHEN ==============
        String tokenValue = emailVerificationService.createVerificationToken(user);

        // ============== THEN ==============
        assertThat(tokenValue).isNotNull().isNotBlank();
        
        // Use ArgumentCaptor to capture and verify the saved token
        ArgumentCaptor<EmailVerificationToken> tokenCaptor = 
                ArgumentCaptor.forClass(EmailVerificationToken.class);
        
        then(tokenRepository).should().save(tokenCaptor.capture());
        
        EmailVerificationToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getToken()).isEqualTo(tokenValue);
        assertThat(savedToken.isUsed()).isFalse();
        assertThat(savedToken.getCreatedAt()).isNotNull();
        assertThat(savedToken.getExpiresAt()).isNotNull();
        
        // Verify expiration is 24 hours from creation (within 1 second tolerance)
        LocalDateTime expectedExpiry = savedToken.getCreatedAt().plusHours(24);
        assertThat(savedToken.getExpiresAt()).isCloseTo(expectedExpiry, within(1, ChronoUnit.SECONDS));
    }

    // ============== EMAIL VERIFICATION TESTS ==============
    // These test the core email verification workflow

    @Test
    void shouldVerifyEmailSuccessfully() {
        // ============== GIVEN ==============
        User unverifiedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .emailVerified(false)
                .build();
        
        String tokenValue = "valid-verification-token";
        
        EmailVerificationToken validToken = EmailVerificationToken.builder()
                .id(1L)
                .user(unverifiedUser)
                .token(tokenValue)
                .used(false)
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .expiresAt(LocalDateTime.now().plusHours(23).plusMinutes(30)) // Still valid
                .build();
        
        // Mock token lookup
        given(tokenRepository.findByToken(tokenValue))
                .willReturn(Optional.of(validToken));
        
        // Mock user and token saving
        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(tokenRepository.save(any(EmailVerificationToken.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // ============== WHEN ==============
        VerificationResult result = emailVerificationService.verifyEmail(tokenValue);

        // ============== THEN ==============
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("Email verified successfully");
        assertThat(result.getUser()).isEqualTo(unverifiedUser);
        
        // Verify user was updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmailVerified()).isTrue();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        
        // Verify token was marked as used
        ArgumentCaptor<EmailVerificationToken> tokenCaptor = 
                ArgumentCaptor.forClass(EmailVerificationToken.class);
        then(tokenRepository).should().save(tokenCaptor.capture());
        
        EmailVerificationToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.isUsed()).isTrue();
    }

    @Test
    void shouldReturnSuccessForAlreadyVerifiedUser() {
        // ============== GIVEN ==============
        User verifiedUser = User.builder()
                .id(1L)
                .email("verified@example.com")
                .emailVerified(true) // Already verified
                .build();
        
        String tokenValue = "some-token";
        
        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(verifiedUser)
                .token(tokenValue)
                .used(false)
                .build();
        
        given(tokenRepository.findByToken(tokenValue))
                .willReturn(Optional.of(token));

        // ============== WHEN ==============
        VerificationResult result = emailVerificationService.verifyEmail(tokenValue);

        // ============== THEN ==============
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("already verified");
        
        // Verify no additional saves occurred since user was already verified
        then(userRepository).should(never()).save(any());
    }

    // ============== ERROR HANDLING TESTS ==============
    // These test various failure scenarios

    @Test
    void shouldHandleInvalidToken() {
        // ============== GIVEN ==============
        String invalidToken = "non-existent-token";
        
        given(tokenRepository.findByToken(invalidToken))
                .willReturn(Optional.empty());

        // ============== WHEN ==============
        VerificationResult result = emailVerificationService.verifyEmail(invalidToken);

        // ============== THEN ==============
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Verification token not found");
        assertThat(result.getUser()).isNull();
        
        then(userRepository).should(never()).save(any());
    }

    @Test
    void shouldHandleExpiredToken() {
        // ============== GIVEN ==============
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .emailVerified(false)
                .build();
        
        String tokenValue = "expired-token";
        
        // Create an expired token (created 25 hours ago, expires after 24 hours)
        EmailVerificationToken expiredToken = EmailVerificationToken.builder()
                .id(1L)
                .user(user)
                .token(tokenValue)
                .used(false)
                .createdAt(LocalDateTime.now().minusHours(25))
                .expiresAt(LocalDateTime.now().minusHours(1)) // Expired 1 hour ago
                .build();
        
        given(tokenRepository.findByToken(tokenValue))
                .willReturn(Optional.of(expiredToken));

        // ============== WHEN ==============
        VerificationResult result = emailVerificationService.verifyEmail(tokenValue);

        // ============== THEN ==============
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("verification link has expired");
        
        // Verify user was not updated due to expired token
        then(userRepository).should(never()).save(any());
    }

    @Test
    void shouldHandleAlreadyUsedToken() {
        // ============== GIVEN ==============
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .emailVerified(false)
                .build();
        
        String tokenValue = "used-token";
        
        EmailVerificationToken usedToken = EmailVerificationToken.builder()
                .id(1L)
                .user(user)
                .token(tokenValue)
                .used(true) // Already used!
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .expiresAt(LocalDateTime.now().plusHours(23).plusMinutes(30))
                .build();
        
        given(tokenRepository.findByToken(tokenValue))
                .willReturn(Optional.of(usedToken));

        // ============== WHEN ==============
        VerificationResult result = emailVerificationService.verifyEmail(tokenValue);

        // ============== THEN ==============
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("already been used");
        
        then(userRepository).should(never()).save(any());
    }

    @Test
    void shouldHandleNullOrEmptyToken() {
        // Test null token
        VerificationResult nullResult = emailVerificationService.verifyEmail(null);
        assertThat(nullResult.isSuccess()).isFalse();
        assertThat(nullResult.getMessage()).contains("Invalid verification token");
        
        // Test empty token
        VerificationResult emptyResult = emailVerificationService.verifyEmail("");
        assertThat(emptyResult.isSuccess()).isFalse();
        assertThat(emptyResult.getMessage()).contains("Invalid verification token");
        
        // Test whitespace token
        VerificationResult whitespaceResult = emailVerificationService.verifyEmail("   ");
        assertThat(whitespaceResult.isSuccess()).isFalse();
        assertThat(whitespaceResult.getMessage()).contains("Invalid verification token");
        
        // Verify no repository calls were made for any invalid input
        then(tokenRepository).should(never()).findByToken(anyString());
    }

    // ============== EXISTING TOKEN REUSE TEST ==============
    // Tests the optimization where existing valid tokens are reused

    @Test
    void shouldReturnExistingValidToken() {
        // ============== GIVEN ==============
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .emailVerified(false)
                .build();
        
        String existingToken = "existing-valid-token";
        
        EmailVerificationToken validExistingToken = EmailVerificationToken.builder()
                .id(1L)
                .user(user)
                .token(existingToken)
                .used(false)
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .expiresAt(LocalDateTime.now().plusHours(23).plusMinutes(30))
                .build();
        
        // Mock rate limiting check (user is under limit)
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(user), any(LocalDateTime.class)))
                .willReturn(1);
        
        // Mock existing valid token
        given(tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(user), any(LocalDateTime.class)))
                .willReturn(Optional.of(validExistingToken));

        // ============== WHEN ==============
        String returnedToken = emailVerificationService.createVerificationToken(user);

        // ============== THEN ==============
        assertThat(returnedToken).isEqualTo(existingToken);
        
        // Verify no new token was created since valid one exists
        then(tokenRepository).should(never()).save(any(EmailVerificationToken.class));
    }

    // ============== EMAIL SERVICE FAILURE TEST ==============
    // Tests handling of external service failures

    @Test
    void shouldHandleEmailServiceFailure() {
        // ============== GIVEN ==============
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .emailVerified(false)
                .build();
        
        // Mock successful token creation
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(user), any(LocalDateTime.class)))
                .willReturn(0);
        given(tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(user), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(tokenRepository.findByToken(anyString()))
                .willReturn(Optional.empty());
        given(tokenRepository.save(any(EmailVerificationToken.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        
        // Mock email service failure
        given(emailService.sendVerificationEmail(eq(user), anyString()))
                .willReturn(false); // Email sending failed

        // ============== WHEN ==============
        boolean result = emailVerificationService.setupAndSendVerification(user);

        // ============== THEN ==============
        assertThat(result).isFalse();
        
        // Verify token was still created even though email failed
        then(tokenRepository).should().save(any(EmailVerificationToken.class));
        // Verify email service was attempted
        then(emailService).should().sendVerificationEmail(eq(user), anyString());
    }

    // ============== VERIFICATION STATUS TESTS ==============
    // Tests the status checking functionality

    @Test
    void shouldReturnCorrectVerificationStatusForVerifiedUser() {
        // ============== GIVEN ==============
        User verifiedUser = User.builder()
                .id(1L)
                .email("verified@example.com")
                .emailVerified(true)
                .build();

        // ============== WHEN ==============
        VerificationStatus status = emailVerificationService.getVerificationStatus(verifiedUser);

        // ============== THEN ==============
        assertThat(status.isVerified()).isTrue();
        assertThat(status.hasPendingToken()).isFalse();
        assertThat(status.message()).contains("Email is verified");
        
        // No repository calls needed for already verified user
        then(tokenRepository).should(never()).findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void shouldReturnCorrectVerificationStatusWithPendingToken() {
        // ============== GIVEN ==============
        User unverifiedUser = User.builder()
                .id(1L)
                .email("unverified@example.com")
                .emailVerified(false)
                .build();
        
        LocalDateTime expiryTime = LocalDateTime.now().plusHours(12);
        EmailVerificationToken pendingToken = EmailVerificationToken.builder()
                .user(unverifiedUser)
                .token("pending-token")
                .used(false)
                .expiresAt(expiryTime)
                .build();
        
        given(tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(unverifiedUser), any(LocalDateTime.class)))
                .willReturn(Optional.of(pendingToken));

        // ============== WHEN ==============
        VerificationStatus status = emailVerificationService.getVerificationStatus(unverifiedUser);

        // ============== THEN ==============
        assertThat(status.isVerified()).isFalse();
        assertThat(status.hasPendingToken()).isTrue();
        assertThat(status.tokenExpiresAt()).isEqualTo(expiryTime);
        assertThat(status.message()).contains("Verification email sent");
    }
}