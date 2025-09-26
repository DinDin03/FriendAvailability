package com.friendavailability.domain.service;

import com.friendavailability.base.BaseUnitTest;
import com.friendavailability.domain.exception.GoogleAuthenticationException;
import com.friendavailability.domain.service.GoogleJwtVerificationService.GoogleUserInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GoogleJwtVerificationService
 * 
 * This test class focuses on OAUTH SECURITY TESTING patterns used at enterprise companies:
 * - JWT token validation and verification
 * - Client ID (audience) validation for security
 * - Token structure and payload validation
 * - OAuth security exception handling
 * - External library integration testing
 * 
 * Learning Focus:
 * - OAuth/JWT security testing patterns
 * - External library mocking strategies
 * - Security exception testing
 * - Token payload validation
 * - Audience verification (critical security check)
 * 
 * Note: This service integrates with Google's JWT library, so we mock the GoogleIdTokenVerifier
 * to test our business logic without making real calls to Google's servers.
 */
class GoogleJwtVerificationServiceTest extends BaseUnitTest {

    // ============== TEST CONSTANTS ==============
    private static final String VALID_CLIENT_ID = "test-client-id";
    private static final String VALID_TOKEN_STRING = "valid.jwt.token.string";
    private static final String GOOGLE_ID = "google-user-123456";
    private static final String EMAIL = "testuser@example.com";
    private static final String NAME = "Test User";

    // ============== MOCKED DEPENDENCIES ==============
    // We mock the Google JWT library components to control behavior in tests
    
    @Mock
    private GoogleIdTokenVerifier mockVerifier;
    
    @Mock
    private GoogleIdToken mockIdToken;
    
    @Mock
    private GoogleIdToken.Payload mockPayload;

    // ============== SERVICE UNDER TEST ==============
    private GoogleJwtVerificationService googleJwtVerificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create service instance with test configuration
        googleJwtVerificationService = new GoogleJwtVerificationService(VALID_CLIENT_ID);
        
        // Use reflection to inject the mocked verifier (since it's created in constructor)
        ReflectionTestUtils.setField(googleJwtVerificationService, "verifier", mockVerifier);
    }

    // ============== HAPPY PATH TESTS ==============
    // These test successful JWT verification scenarios

    @Test
    void shouldVerifyValidJwtTokenSuccessfully() throws GeneralSecurityException, IOException {
        // ============== GIVEN ==============
        // Mock successful token verification
        given(mockVerifier.verify(VALID_TOKEN_STRING))
                .willReturn(mockIdToken);
        
        // Mock token payload extraction
        given(mockIdToken.getPayload())
                .willReturn(mockPayload);
        
        // Mock payload data - this is what Google sends us
        given(mockPayload.getAudience())
                .willReturn(VALID_CLIENT_ID); // Critical: audience must match our client ID
        given(mockPayload.getSubject())
                .willReturn(GOOGLE_ID); // Google's unique user ID
        given(mockPayload.getEmail())
                .willReturn(EMAIL);
        given(mockPayload.get("name"))
                .willReturn(NAME);
        given(mockPayload.getEmailVerified())
                .willReturn(true); // Google has verified this email

        // ============== WHEN ==============
        GoogleUserInfo result = googleJwtVerificationService.verifyToken(VALID_TOKEN_STRING);

        // ============== THEN ==============
        // Verify all user information was extracted correctly
        assertThat(result).isNotNull();
        assertThat(result.getGoogleId()).isEqualTo(GOOGLE_ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getName()).isEqualTo(NAME);
        assertThat(result.isEmailVerified()).isTrue();
        
        // Verify Google library was called correctly
        then(mockVerifier).should().verify(VALID_TOKEN_STRING);
        then(mockIdToken).should().getPayload();
        then(mockPayload).should().getAudience();
        then(mockPayload).should().getSubject();
        then(mockPayload).should().getEmail();
        then(mockPayload).should().get("name");
        then(mockPayload).should().getEmailVerified();
    }

    @Test
    void shouldHandleNullEmailVerificationGracefully() throws GeneralSecurityException, IOException {
        // ============== GIVEN ==============
        // Some Google tokens might not have email verification status
        given(mockVerifier.verify(VALID_TOKEN_STRING)).willReturn(mockIdToken);
        given(mockIdToken.getPayload()).willReturn(mockPayload);
        given(mockPayload.getAudience()).willReturn(VALID_CLIENT_ID);
        given(mockPayload.getSubject()).willReturn(GOOGLE_ID);
        given(mockPayload.getEmail()).willReturn(EMAIL);
        given(mockPayload.get("name")).willReturn(NAME);
        given(mockPayload.getEmailVerified())
                .willReturn(null); // No email verification status

        // ============== WHEN ==============
        GoogleUserInfo result = googleJwtVerificationService.verifyToken(VALID_TOKEN_STRING);

        // ============== THEN ==============
        // Should default to false when null
        assertThat(result.isEmailVerified()).isFalse();
        assertThat(result.getGoogleId()).isEqualTo(GOOGLE_ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getName()).isEqualTo(NAME);
    }

    @Test
    void shouldHandleEmailVerifiedFalse() throws GeneralSecurityException, IOException {
        // ============== GIVEN ==============
        given(mockVerifier.verify(VALID_TOKEN_STRING)).willReturn(mockIdToken);
        given(mockIdToken.getPayload()).willReturn(mockPayload);
        given(mockPayload.getAudience()).willReturn(VALID_CLIENT_ID);
        given(mockPayload.getSubject()).willReturn(GOOGLE_ID);
        given(mockPayload.getEmail()).willReturn(EMAIL);
        given(mockPayload.get("name")).willReturn(NAME);
        given(mockPayload.getEmailVerified())
                .willReturn(false); // Google says email is not verified

        // ============== WHEN ==============
        GoogleUserInfo result = googleJwtVerificationService.verifyToken(VALID_TOKEN_STRING);

        // ============== THEN ==============
        assertThat(result.isEmailVerified()).isFalse();
        assertThat(result.getGoogleId()).isEqualTo(GOOGLE_ID);
    }

    // ============== INPUT VALIDATION TESTS ==============
    // These test various invalid input scenarios

    @Test
    void shouldThrowExceptionForNullToken() {
        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> googleJwtVerificationService.verifyToken(null))
                .isInstanceOf(GoogleAuthenticationException.class)
                .hasMessageContaining("missing credential");
        
        // Verify no external calls were made for null input
        then(mockVerifier).should(never()).verify(anyString());
    }

    @Test
    void shouldThrowExceptionForEmptyToken() {
        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> googleJwtVerificationService.verifyToken(""))
                .isInstanceOf(GoogleAuthenticationException.class)
                .hasMessageContaining("missing credential");
        
        then(mockVerifier).should(never()).verify(anyString());
    }

    @Test
    void shouldThrowExceptionForWhitespaceToken() {
        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> googleJwtVerificationService.verifyToken("   "))
                .isInstanceOf(GoogleAuthenticationException.class)
                .hasMessageContaining("missing credential");
        
        then(mockVerifier).should(never()).verify(anyString());
    }

    // ============== JWT VALIDATION TESTS ==============
    // These test various JWT token validation failures

    @Test
    void shouldThrowExceptionForInvalidJwtToken() throws GeneralSecurityException, IOException {
        // ============== GIVEN ==============
        String invalidToken = "invalid.jwt.token";
        
        // Google's library returns null for invalid tokens
        given(mockVerifier.verify(invalidToken))
                .willReturn(null);

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> googleJwtVerificationService.verifyToken(invalidToken))
                .isInstanceOf(GoogleAuthenticationException.class)
                .hasMessageContaining("invalid token");
        
        then(mockVerifier).should().verify(invalidToken);
    }

    @Test
    void shouldThrowExceptionForMalformedToken() throws GeneralSecurityException, IOException {
        // ============== GIVEN ==============
        String malformedToken = "malformed.token";
        
        // Google's library throws GeneralSecurityException for malformed tokens
        given(mockVerifier.verify(malformedToken))
                .willThrow(new GeneralSecurityException("Token is malformed"));

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> googleJwtVerificationService.verifyToken(malformedToken))
                .isInstanceOf(GoogleAuthenticationException.class)
                .hasMessageContaining("token verification failed")
                .hasMessageContaining("Token is malformed");
        
        then(mockVerifier).should().verify(malformedToken);
    }

    @Test
    void shouldThrowExceptionForNetworkError() throws GeneralSecurityException, IOException {
        // ============== GIVEN ==============
        String validToken = "valid.token";
        
        // Google's library throws IOException for network issues
        given(mockVerifier.verify(validToken))
                .willThrow(new IOException("Network timeout"));

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> googleJwtVerificationService.verifyToken(validToken))
                .isInstanceOf(GoogleAuthenticationException.class)
                .hasMessageContaining("token verification failed")
                .hasMessageContaining("Network timeout");
        
        then(mockVerifier).should().verify(validToken);
    }

    // ============== AUDIENCE VALIDATION TESTS (Critical Security) ==============
    // These test client ID validation - prevents token reuse across applications

    @Test
    void shouldThrowExceptionForWrongAudience() throws GeneralSecurityException, IOException {
        // ============== GIVEN ==============
        String wrongClientId = "wrong-client-id";
        
        given(mockVerifier.verify(VALID_TOKEN_STRING)).willReturn(mockIdToken);
        given(mockIdToken.getPayload()).willReturn(mockPayload);
        given(mockPayload.getAudience())
                .willReturn(wrongClientId); // Token intended for different app

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> googleJwtVerificationService.verifyToken(VALID_TOKEN_STRING))
                .isInstanceOf(GoogleAuthenticationException.class)
                .hasMessageContaining("invalid audience")
                .hasMessageContaining(VALID_CLIENT_ID) // Expected client ID
                .hasMessageContaining(wrongClientId); // Actual client ID
        
        // Verify we checked the audience but didn't extract user data
        then(mockPayload).should().getAudience();
        then(mockPayload).should(never()).getSubject();
        then(mockPayload).should(never()).getEmail();
    }

    @Test
    void shouldThrowExceptionForNullAudience() throws GeneralSecurityException, IOException {
        // ============== GIVEN ==============
        given(mockVerifier.verify(VALID_TOKEN_STRING)).willReturn(mockIdToken);
        given(mockIdToken.getPayload()).willReturn(mockPayload);
        given(mockPayload.getAudience())
                .willReturn(null); // No audience in token

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> googleJwtVerificationService.verifyToken(VALID_TOKEN_STRING))
                .isInstanceOf(GoogleAuthenticationException.class)
                .hasMessageContaining("invalid audience");
        
        then(mockPayload).should().getAudience();
        then(mockPayload).should(never()).getSubject();
    }

    // ============== PAYLOAD EXTRACTION TESTS ==============
    // These test various edge cases in token payload processing

    @Test
    void shouldHandleNullNameGracefully() throws GeneralSecurityException, IOException {
        // ============== GIVEN ==============
        given(mockVerifier.verify(VALID_TOKEN_STRING)).willReturn(mockIdToken);
        given(mockIdToken.getPayload()).willReturn(mockPayload);
        given(mockPayload.getAudience()).willReturn(VALID_CLIENT_ID);
        given(mockPayload.getSubject()).willReturn(GOOGLE_ID);
        given(mockPayload.getEmail()).willReturn(EMAIL);
        given(mockPayload.get("name"))
                .willReturn(null); // Some tokens might not have name
        given(mockPayload.getEmailVerified()).willReturn(true);

        // ============== WHEN ==============
        GoogleUserInfo result = googleJwtVerificationService.verifyToken(VALID_TOKEN_STRING);

        // ============== THEN ==============
        assertThat(result.getName()).isNull();
        assertThat(result.getGoogleId()).isEqualTo(GOOGLE_ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.isEmailVerified()).isTrue();
    }

    @Test
    void shouldHandleEmptyStringName() throws GeneralSecurityException, IOException {
        // ============== GIVEN ==============
        given(mockVerifier.verify(VALID_TOKEN_STRING)).willReturn(mockIdToken);
        given(mockIdToken.getPayload()).willReturn(mockPayload);
        given(mockPayload.getAudience()).willReturn(VALID_CLIENT_ID);
        given(mockPayload.getSubject()).willReturn(GOOGLE_ID);
        given(mockPayload.getEmail()).willReturn(EMAIL);
        given(mockPayload.get("name"))
                .willReturn(""); // Empty name
        given(mockPayload.getEmailVerified()).willReturn(true);

        // ============== WHEN ==============
        GoogleUserInfo result = googleJwtVerificationService.verifyToken(VALID_TOKEN_STRING);

        // ============== THEN ==============
        assertThat(result.getName()).isEmpty();
        assertThat(result.getGoogleId()).isEqualTo(GOOGLE_ID);
    }

    @Test
    void shouldExtractCompleteUserInformationWithAllFields() throws GeneralSecurityException, IOException {
        // ============== GIVEN ==============
        String longGoogleId = "very-long-google-user-id-123456789";
        String complexEmail = "complex.email+test@example-domain.com";
        String complexName = "João da Silva Santos Jr."; // Test Unicode name
        
        given(mockVerifier.verify(VALID_TOKEN_STRING)).willReturn(mockIdToken);
        given(mockIdToken.getPayload()).willReturn(mockPayload);
        given(mockPayload.getAudience()).willReturn(VALID_CLIENT_ID);
        given(mockPayload.getSubject()).willReturn(longGoogleId);
        given(mockPayload.getEmail()).willReturn(complexEmail);
        given(mockPayload.get("name")).willReturn(complexName);
        given(mockPayload.getEmailVerified()).willReturn(true);

        // ============== WHEN ==============
        GoogleUserInfo result = googleJwtVerificationService.verifyToken(VALID_TOKEN_STRING);

        // ============== THEN ==============
        assertThat(result.getGoogleId()).isEqualTo(longGoogleId);
        assertThat(result.getEmail()).isEqualTo(complexEmail);
        assertThat(result.getName()).isEqualTo(complexName);
        assertThat(result.isEmailVerified()).isTrue();
    }

    // ============== GOOGLEUSERINFO CLASS TESTS ==============
    // These test the inner GoogleUserInfo data class

    @Test
    void shouldCreateGoogleUserInfoWithAllParameters() {
        // ============== GIVEN ==============
        String googleId = "test-google-id";
        String email = "test@example.com";
        String name = "Test User";
        Boolean emailVerified = true;

        // ============== WHEN ==============
        GoogleUserInfo userInfo = new GoogleUserInfo(googleId, email, name, emailVerified);

        // ============== THEN ==============
        assertThat(userInfo.getGoogleId()).isEqualTo(googleId);
        assertThat(userInfo.getEmail()).isEqualTo(email);
        assertThat(userInfo.getName()).isEqualTo(name);
        assertThat(userInfo.isEmailVerified()).isTrue();
    }

    @Test
    void shouldHandleNullEmailVerifiedInConstructor() {
        // ============== GIVEN ==============
        String googleId = "test-google-id";
        String email = "test@example.com";
        String name = "Test User";
        Boolean emailVerified = null; // Null Boolean

        // ============== WHEN ==============
        GoogleUserInfo userInfo = new GoogleUserInfo(googleId, email, name, emailVerified);

        // ============== THEN ==============
        assertThat(userInfo.isEmailVerified()).isFalse(); // Should default to false
    }

    @Test
    void shouldGenerateToStringCorrectly() {
        // ============== GIVEN ==============
        GoogleUserInfo userInfo = new GoogleUserInfo("id123", "user@example.com", "User Name", true);

        // ============== WHEN ==============
        String result = userInfo.toString();

        // ============== THEN ==============
        assertThat(result).contains("GoogleUserInfo");
        assertThat(result).contains("googleId='id123'");
        assertThat(result).contains("email='user@example.com'");
        assertThat(result).contains("name='User Name'");
        assertThat(result).contains("emailVerified=true");
    }

    // ============== INTEGRATION-STYLE TESTS ==============
    // These test the complete flow from token string to user info

    @Test
    void shouldCompleteFullVerificationWorkflow() throws GeneralSecurityException, IOException {
        // ============== GIVEN ==============
        // This test verifies the complete workflow from token to user info
        String realWorldToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6InRlc3QifQ...";
        String realGoogleId = "115990123456789012345";
        String realEmail = "realuser@gmail.com";
        String realName = "Real User";
        
        given(mockVerifier.verify(realWorldToken)).willReturn(mockIdToken);
        given(mockIdToken.getPayload()).willReturn(mockPayload);
        given(mockPayload.getAudience()).willReturn(VALID_CLIENT_ID);
        given(mockPayload.getSubject()).willReturn(realGoogleId);
        given(mockPayload.getEmail()).willReturn(realEmail);
        given(mockPayload.get("name")).willReturn(realName);
        given(mockPayload.getEmailVerified()).willReturn(true);

        // ============== WHEN ==============
        GoogleUserInfo result = googleJwtVerificationService.verifyToken(realWorldToken);

        // ============== THEN ==============
        // Verify complete user information extraction
        assertThat(result.getGoogleId()).isEqualTo(realGoogleId);
        assertThat(result.getEmail()).isEqualTo(realEmail);
        assertThat(result.getName()).isEqualTo(realName);
        assertThat(result.isEmailVerified()).isTrue();
        
        // Verify all security checks were performed
        then(mockVerifier).should().verify(realWorldToken);
        then(mockPayload).should().getAudience(); // Critical security check
        then(mockPayload).should().getSubject();
        then(mockPayload).should().getEmail();
        then(mockPayload).should().get("name");
        then(mockPayload).should().getEmailVerified();
    }

    // ============== ERROR HANDLING COMPREHENSIVE TEST ==============
    // This tests multiple error scenarios in sequence

    @Test
    void shouldHandleMultipleErrorScenarios() {
        // Test 1: Null token
        assertThatThrownBy(() -> googleJwtVerificationService.verifyToken(null))
                .isInstanceOf(GoogleAuthenticationException.class);
        
        // Test 2: Empty token
        assertThatThrownBy(() -> googleJwtVerificationService.verifyToken(""))
                .isInstanceOf(GoogleAuthenticationException.class);
        
        // Test 3: Whitespace token
        assertThatThrownBy(() -> googleJwtVerificationService.verifyToken("   "))
                .isInstanceOf(GoogleAuthenticationException.class);
        
        // Verify no external calls were made for any invalid input
        then(mockVerifier).should(never()).verify(anyString());
    }
}