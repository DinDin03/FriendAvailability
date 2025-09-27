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

    @InjectMocks
    private PasswordResetService passwordResetService;

    // ============== TOKEN VERIFICATION TESTS ==============

    @Test
    void shouldValidateResetTokenSuccessfully() {
        // Given
        String tokenValue = "valid-reset-token-123";
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("Test User")
                .emailVerified(true)
                .isActive(true)
                .build();

        PasswordResetToken validToken = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .token(tokenValue)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        given(tokenRepository.findByToken(tokenValue)).willReturn(Optional.of(validToken));

        // When
        User result = passwordResetService.validateResetToken(tokenValue);

        // Then
        assertThat(result).isEqualTo(user);
        assertThat(result.getEmail()).isEqualTo("user@example.com");
        then(tokenRepository).should().findByToken(tokenValue);
    }

    @Test
    void shouldThrowExceptionForNullToken() {
        // When & Then
        assertThatThrownBy(() -> passwordResetService.validateResetToken(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password reset token is required");
    }

    @Test
    void shouldThrowExceptionForEmptyToken() {
        // When & Then
        assertThatThrownBy(() -> passwordResetService.validateResetToken(""))
                .isInstanceOf(StringIndexOutOfBoundsException.class);
    }

    @Test
    void shouldThrowExceptionForNonExistentToken() {
        // Given
        String nonExistentToken = "non-existent-token";
        given(tokenRepository.findByToken(nonExistentToken)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> passwordResetService.validateResetToken(nonExistentToken))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Password reset token not found");

        then(tokenRepository).should().findByToken(nonExistentToken);
    }

    @Test
    void shouldThrowExceptionForUsedToken() {
        // Given
        String usedToken = "already-used-token";
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();

        PasswordResetToken usedPasswordToken = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .token(usedToken)
                .used(true) // Already used
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        given(tokenRepository.findByToken(usedToken)).willReturn(Optional.of(usedPasswordToken));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.validateResetToken(usedToken))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Password reset token has already been used");

        then(tokenRepository).should().findByToken(usedToken);
    }

    @Test
    void shouldThrowExceptionForExpiredToken() {
        // Given
        String expiredToken = "expired-token";
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();

        PasswordResetToken expiredPasswordToken = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .token(expiredToken)
                .used(false)
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // Expired 5 minutes ago
                .build();

        given(tokenRepository.findByToken(expiredToken)).willReturn(Optional.of(expiredPasswordToken));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.validateResetToken(expiredToken))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Password reset token has expired");

        then(tokenRepository).should().findByToken(expiredToken);
    }

    // ============== PASSWORD RESET TESTS ==============

    @Test
    void shouldResetPasswordSuccessfully() {
        // Given
        String tokenValue = "valid-reset-token";
        String newPassword = "newSecurePassword123";
        String encodedPassword = "encoded-new-password";

        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("old-password-hash")
                .build();

        PasswordResetToken validToken = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .token(tokenValue)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        given(tokenRepository.findByToken(tokenValue)).willReturn(Optional.of(validToken));
        given(passwordEncoder.encode(newPassword)).willReturn(encodedPassword);
        given(userRepository.save(any(User.class))).willReturn(user);
        given(tokenRepository.save(any(PasswordResetToken.class))).willReturn(validToken);

        // When
        passwordResetService.resetPassword(tokenValue, newPassword);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        then(passwordEncoder).should().encode(newPassword);
        then(userRepository).should().save(userCaptor.capture());
        then(tokenRepository).should().save(tokenCaptor.capture());

        User savedUser = userCaptor.getValue();
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertThat(savedUser.getPasswordHash()).isEqualTo(encodedPassword);
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedToken.isUsed()).isTrue();
    }

    @Test
    void shouldThrowExceptionForInvalidPasswordResetInput() {
        // Test null token
        assertThatThrownBy(() -> passwordResetService.resetPassword(null, "validPassword123"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password reset token is required");

        // Test empty token
        assertThatThrownBy(() -> passwordResetService.resetPassword("", "validPassword123"))
                .isInstanceOf(StringIndexOutOfBoundsException.class);

        // Test null password
        assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("New password is required");

        // Test empty password
        assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token", ""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("New password is required");

        // Test short password
        assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token", "short"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password must be at least 8 characters");
    }

    // ============== TOKEN CREATION TESTS ==============

    @Test
    void shouldCreateResetTokenSuccessfully() {
        // Given
        User verifiedUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .emailVerified(true)
                .isActive(true)
                .build();

        // Mock rate limiting check - use matchers for time-sensitive operations
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(verifiedUser), any(LocalDateTime.class))).willReturn(2); // Under limit

        // Mock no existing valid token
        given(tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(verifiedUser), any(LocalDateTime.class))).willReturn(Optional.empty());

        // Mock token uniqueness check
        given(tokenRepository.findByToken(any(String.class))).willReturn(Optional.empty());

        // Mock token saving
        given(tokenRepository.save(any(PasswordResetToken.class)))
                .willAnswer(invocation -> {
                    PasswordResetToken token = invocation.getArgument(0);
                    return PasswordResetToken.builder()
                            .id(1L)
                            .user(token.getUser())
                            .token(token.getToken())
                            .expiresAt(token.getExpiresAt())
                            .createdAt(token.getCreatedAt())
                            .used(token.isUsed())
                            .build();
                });

        // When
        String actualToken = passwordResetService.createResetToken(verifiedUser);

        // Then
        assertThat(actualToken).isNotNull();
        assertThat(actualToken).isNotEmpty();

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        then(tokenRepository).should().save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(verifiedUser);
        assertThat(savedToken.isUsed()).isFalse();
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(25));
    }

    @Test
    void shouldThrowExceptionForNullUser() {
        // When & Then
        User nullUser = null;
        assertThatThrownBy(() -> passwordResetService.createResetToken(nullUser))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldThrowExceptionForUnverifiedEmail() {
        // Given
        User unverifiedUser = User.builder()
                .id(1L)
                .email("unverified@example.com")
                .emailVerified(false)
                .build();

        // When & Then
        assertThatThrownBy(() -> passwordResetService.createResetToken(unverifiedUser))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password reset is only available for verified email addresses");
    }

    @Test
    void shouldThrowExceptionWhenRateLimitExceeded() {
        // Given
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .emailVerified(true)
                .build();

        // Mock rate limiting check - exceeding limit - use matchers for time-sensitive operations
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(user), any(LocalDateTime.class))).willReturn(4); // Over limit

        // When & Then
        assertThatThrownBy(() -> passwordResetService.createResetToken(user))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Too many password reset attempts");
    }

    @Test
    void shouldReturnExistingValidToken() {
        // Given
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .emailVerified(true)
                .build();

        String existingToken = "existing-valid-token";
        PasswordResetToken validToken = PasswordResetToken.builder()
                .token(existingToken)
                .user(user)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .build();

        // Mock rate limiting check - use matchers for time-sensitive operations
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(user), any(LocalDateTime.class))).willReturn(1);

        // Mock existing valid token
        given(tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(user), any(LocalDateTime.class))).willReturn(Optional.of(validToken));

        // When
        String result = passwordResetService.createResetToken(user);

        // Then
        assertThat(result).isEqualTo(existingToken);

        // Verify no new token was created
        then(tokenRepository).should(never()).save(any(PasswordResetToken.class));
    }

    // ============== PASSWORD RESET REQUEST TESTS ==============

    @Test
    void shouldRequestPasswordResetSuccessfully() {
        // Given
        String email = "user@example.com";
        User user = User.builder()
                .id(1L)
                .email(email)
                .emailVerified(true)
                .isActive(true)
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(tokenRepository.countByUserAndCreatedAtAfter(eq(user), any(LocalDateTime.class))).willReturn(1);
        given(tokenRepository.findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(user), any(LocalDateTime.class))).willReturn(Optional.empty());
        given(tokenRepository.findByToken(any(String.class))).willReturn(Optional.empty());
        given(tokenRepository.save(any(PasswordResetToken.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(emailService.sendPasswordResetEmail(eq(user), any(String.class))).willReturn(true);

        // When - should not throw exception
        passwordResetService.requestPasswordReset(email);

        // Then
        then(userRepository).should().findByEmail(email);
        then(emailService).should().sendPasswordResetEmail(eq(user), any(String.class));
    }

    @Test
    void shouldHandleNonExistentEmailGracefully() {
        // Given
        String nonExistentEmail = "nonexistent@example.com";
        given(userRepository.findByEmail(nonExistentEmail)).willReturn(Optional.empty());

        // When - should not throw exception for security reasons
        passwordResetService.requestPasswordReset(nonExistentEmail);

        // Then
        then(userRepository).should().findByEmail(nonExistentEmail);
        then(emailService).should(never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void shouldThrowExceptionForNullOrEmptyEmail() {
        // Test null email
        assertThatThrownBy(() -> passwordResetService.requestPasswordReset(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email address is required for password reset");

        // Test empty email
        assertThatThrownBy(() -> passwordResetService.requestPasswordReset(""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email address is required for password reset");

        // Test whitespace email
        assertThatThrownBy(() -> passwordResetService.requestPasswordReset("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email address is required for password reset");
    }

    // ============== CLEANUP TESTS ==============

    @Test
    void shouldCleanExpiredTokensSuccessfully() {
        // Given
        int expectedDeletedCount = 5;
        given(tokenRepository.deleteExpiredTokens(any(LocalDateTime.class))).willReturn(expectedDeletedCount);

        // When
        int actualDeletedCount = passwordResetService.cleanExpiredTokens();

        // Then
        assertThat(actualDeletedCount).isEqualTo(expectedDeletedCount);
        then(tokenRepository).should().deleteExpiredTokens(any(LocalDateTime.class));
    }

    @Test
    void shouldHandleNoExpiredTokensToClean() {
        // Given
        given(tokenRepository.deleteExpiredTokens(any(LocalDateTime.class))).willReturn(0);

        // When
        int deletedCount = passwordResetService.cleanExpiredTokens();

        // Then
        assertThat(deletedCount).isEqualTo(0);
        then(tokenRepository).should().deleteExpiredTokens(any(LocalDateTime.class));
    }

}