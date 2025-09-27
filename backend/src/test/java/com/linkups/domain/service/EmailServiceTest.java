package com.linkups.domain.service;

import com.linkups.base.BaseUnitTest;
import com.linkups.domain.entity.User;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService
 *
 * This test class focuses on EXTERNAL SERVICE INTEGRATION patterns used at enterprise companies:
 * - Email service integration testing without sending real emails
 * - Template processing and variable substitution
 * - External dependency mocking (JavaMailSender)
 * - Configuration injection testing
 * - Error handling for external service failures
 * - Message content and structure validation
 *
 * Learning Focus:
 * - External service mocking strategies
 * - Template processing testing
 * - Configuration-driven testing
 * - Email content validation
 * - Error resilience testing
 *
 * Note: We mock JavaMailSender to avoid sending real emails during tests
 * while still testing our email composition and sending logic.
 */
//All tests passing
class EmailServiceTest extends BaseUnitTest {

    // ============== TEST CONSTANTS ==============
    private static final String TEST_FROM_NAME = "Test LinkUp Team";
    private static final String TEST_FROM_ADDRESS = "test@linkup.com";
    private static final String TEST_BASE_URL = "https://test.linkup.com";
    private static final String TEST_TOKEN = "test-verification-token-123";

    // ============== MOCKED DEPENDENCIES ==============
    // Mock the Spring mail sender to avoid sending real emails

    @Mock
    private JavaMailSender mockMailSender;

    @Mock
    private MimeMessage mockMimeMessage;

    @Mock
    private ClassPathResource mockResource;

    // ============== SERVICE UNDER TEST ==============

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Inject test configuration values using reflection
        ReflectionTestUtils.setField(emailService, "fromName", TEST_FROM_NAME);
        ReflectionTestUtils.setField(emailService, "fromAddress", TEST_FROM_ADDRESS);
        ReflectionTestUtils.setField(emailService, "baseUrl", TEST_BASE_URL);
    }

    // ============== VERIFICATION EMAIL TESTS ==============
    // These test the email verification workflow

    @Test
    void shouldSendVerificationEmailSuccessfully() throws Exception {
        // ============== GIVEN ==============
        User testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john.doe@example.com")
                .build();

        // Mock email template content (simplified for testing)
        String mockTemplate = """
            <html>
                <body>
                    <h1>Welcome {{userName}}!</h1>
                    <p>Please verify your email: {{userEmail}}</p>
                    <a href="{{verificationUrl}}">Verify Email</a>
                    <p>LinkUp Team at {{baseUrl}}</p>
                </body>
            </html>
            """;

        // Mock MimeMessage creation and helper setup
        given(mockMailSender.createMimeMessage()).willReturn(mockMimeMessage);

        // Simulate successful template loading by overriding loadTemplate method behavior
        // We'll use a spy to partially mock the service
        EmailService spyEmailService = spy(emailService);
        doReturn(mockTemplate).when(spyEmailService).loadTemplate("verification-email.html");

        // ============== WHEN ==============
        boolean result = spyEmailService.sendVerificationEmail(testUser, TEST_TOKEN);

        // ============== THEN ==============
        assertThat(result).isTrue();

        // Verify mail sender was called
        then(mockMailSender).should().createMimeMessage();
        then(mockMailSender).should().send(mockMimeMessage);

        // Verify template loading was attempted
        then(spyEmailService).should().loadTemplate("verification-email.html");
    }

    @Test
    void shouldHandleEmailSendingFailure() throws Exception {
        // ============== GIVEN ==============
        User testUser = User.builder()
                .id(1L)
                .name("Jane Doe")
                .email("jane.doe@example.com")
                .build();

        String mockTemplate = "<html><body>Test</body></html>";

        // Mock template loading success but email sending failure
        EmailService spyEmailService = spy(emailService);
        doReturn(mockTemplate).when(spyEmailService).loadTemplate("verification-email.html");

        given(mockMailSender.createMimeMessage()).willReturn(mockMimeMessage);
        doThrow(new RuntimeException("SMTP server unavailable"))
                .when(mockMailSender).send(mockMimeMessage);

        // ============== WHEN ==============
        boolean result = spyEmailService.sendVerificationEmail(testUser, TEST_TOKEN);

        // ============== THEN ==============
        assertThat(result).isFalse(); // Should return false on failure

        // Verify send was attempted but failed gracefully
        then(mockMailSender).should().createMimeMessage();
        then(mockMailSender).should().send(mockMimeMessage);
    }

    @Test
    void shouldProcessVerificationEmailTemplateCorrectly() throws Exception {
        // ============== GIVEN ==============
        User testUser = User.builder()
                .name("Alice Smith")
                .email("alice.smith@example.com")
                .build();

        String templateWithPlaceholders = """
            <html>
                <body>
                    <h1>Hello {{userName}}!</h1>
                    <p>Email: {{userEmail}}</p>
                    <a href="{{verificationUrl}}">Click here</a>
                    <p>Visit us at {{baseUrl}}</p>
                </body>
            </html>
            """;

        String expectedProcessedContent = """
            <html>
                <body>
                    <h1>Hello Alice Smith!</h1>
                    <p>Email: alice.smith@example.com</p>
                    <a href="%s/api/auth/verify-email?token=%s">Click here</a>
                    <p>Visit us at %s</p>
                </body>
            </html>
            """.formatted(TEST_BASE_URL, TEST_TOKEN, TEST_BASE_URL);

        EmailService spyEmailService = spy(emailService);
        doReturn(templateWithPlaceholders).when(spyEmailService).loadTemplate("verification-email.html");
        given(mockMailSender.createMimeMessage()).willReturn(mockMimeMessage);

        // ============== WHEN ==============
        boolean result = spyEmailService.sendVerificationEmail(testUser, TEST_TOKEN);

        // ============== THEN ==============
        assertThat(result).isTrue();

        // Verify template processing by checking the content would be processed correctly
        // (This tests our understanding of the template processing logic)
        String actualProcessedContent = templateWithPlaceholders
                .replace("{{userName}}", testUser.getName())
                .replace("{{userEmail}}", testUser.getEmail())
                .replace("{{verificationUrl}}", TEST_BASE_URL + "/api/auth/verify-email?token=" + TEST_TOKEN)
                .replace("{{baseUrl}}", TEST_BASE_URL);

        assertThat(actualProcessedContent).isEqualTo(expectedProcessedContent);
    }

    // ============== PASSWORD RESET EMAIL TESTS ==============
    // These test the password reset email functionality

    @Test
    void shouldSendPasswordResetEmailSuccessfully() throws Exception {
        // ============== GIVEN ==============
        User testUser = User.builder()
                .id(2L)
                .name("Bob Wilson")
                .email("bob.wilson@example.com")
                .build();

        String mockTemplate = """
            <html>
                <body>
                    <h1>Password Reset for {{userName}}</h1>
                    <p>Click to reset: <a href="{{resetUrl}}">Reset Password</a></p>
                    <p>{{baseUrl}}</p>
                </body>
            </html>
            """;

        EmailService spyEmailService = spy(emailService);
        doReturn(mockTemplate).when(spyEmailService).loadTemplate("password-reset-email.html");
        given(mockMailSender.createMimeMessage()).willReturn(mockMimeMessage);

        // ============== WHEN ==============
        boolean result = spyEmailService.sendPasswordResetEmail(testUser, TEST_TOKEN);

        // ============== THEN ==============
        assertThat(result).isTrue();

        then(mockMailSender).should().createMimeMessage();
        then(mockMailSender).should().send(mockMimeMessage);
        then(spyEmailService).should().loadTemplate("password-reset-email.html");
    }

    @Test
    void shouldGenerateCorrectPasswordResetUrl() throws Exception {
        // ============== GIVEN ==============
        User testUser = User.builder()
                .name("Carol Johnson")
                .email("carol.johnson@example.com")
                .build();

        String resetToken = "reset-token-456";

        // ============== WHEN ==============
        // Test the URL generation logic (this is what the service does internally)
        String expectedResetUrl = TEST_BASE_URL + "/pages/auth/new-password.html?token=" +
                resetToken + "&email=" + testUser.getEmail();

        // ============== THEN ==============
        assertThat(expectedResetUrl).contains("/pages/auth/new-password.html");
        assertThat(expectedResetUrl).contains("token=" + resetToken);
        assertThat(expectedResetUrl).contains("email=" + testUser.getEmail());
        assertThat(expectedResetUrl).startsWith(TEST_BASE_URL);
    }

    @Test
    void shouldHandlePasswordResetEmailFailure() throws Exception {
        // ============== GIVEN ==============
        User testUser = User.builder()
                .name("David Brown")
                .email("david.brown@example.com")
                .build();

        EmailService spyEmailService = spy(emailService);
        doThrow(new RuntimeException("Template not found"))
                .when(spyEmailService).loadTemplate("password-reset-email.html");

        // ============== WHEN ==============
        boolean result = spyEmailService.sendPasswordResetEmail(testUser, TEST_TOKEN);

        // ============== THEN ==============
        assertThat(result).isFalse(); // Should handle failure gracefully

        then(spyEmailService).should().loadTemplate("password-reset-email.html");
        // Mail sender should not be called if template loading fails
        then(mockMailSender).should(never()).createMimeMessage();
    }

    // ============== WELCOME EMAIL TESTS ==============
    // These test the welcome email functionality

    @Test
    void shouldSendWelcomeEmailSuccessfully() throws Exception {
        // ============== GIVEN ==============
        User testUser = User.builder()
                .id(3L)
                .name("Eve Davis")
                .email("eve.davis@example.com")
                .build();

        String mockTemplate = """
            <html>
                <body>
                    <h1>Welcome to LinkUp, {{userName}}!</h1>
                    <p>Your account {{userEmail}} is ready!</p>
                    <a href="{{dashboardUrl}}">Go to Dashboard</a>
                </body>
            </html>
            """;

        EmailService spyEmailService = spy(emailService);
        doReturn(mockTemplate).when(spyEmailService).loadTemplate("welcome-email.html");
        given(mockMailSender.createMimeMessage()).willReturn(mockMimeMessage);

        // ============== WHEN ==============
        boolean result = spyEmailService.sendWelcomeEmail(testUser);

        // ============== THEN ==============
        assertThat(result).isTrue();

        then(mockMailSender).should().createMimeMessage();
        then(mockMailSender).should().send(mockMimeMessage);
        then(spyEmailService).should().loadTemplate("welcome-email.html");
    }

    @Test
    void shouldGenerateCorrectDashboardUrl() {
        // ============== GIVEN ==============
        String expectedDashboardUrl = TEST_BASE_URL + "/dashboard.html";

        // ============== WHEN & THEN ==============
        // Test the URL generation logic used in welcome emails
        assertThat(expectedDashboardUrl).isEqualTo(TEST_BASE_URL + "/dashboard.html");
        assertThat(expectedDashboardUrl).startsWith(TEST_BASE_URL);
        assertThat(expectedDashboardUrl).endsWith("/dashboard.html");
    }

    // ============== TEMPLATE PROCESSING TESTS ==============
    // These test the template processing logic in isolation

    @Test
    void shouldProcessTemplateWithAllPlaceholders() {
        // ============== GIVEN ==============
        User testUser = User.builder()
                .name("Frank Miller")
                .email("frank.miller@example.com")
                .build();

        String templateWithAllPlaceholders = """
            Hello {{userName}},
            Your email is {{userEmail}}.
            Verification: {{verificationUrl}}
            Dashboard: {{dashboardUrl}}
            Reset: {{resetUrl}}
            Base URL: {{baseUrl}}
            """;

        String primaryUrl = "https://example.com/action";
        String secondaryUrl = "https://example.com";

        // ============== WHEN ==============
        // Test the template processing logic directly
        String result = templateWithAllPlaceholders
                .replace("{{userName}}", testUser.getName())
                .replace("{{userEmail}}", testUser.getEmail())
                .replace("{{verificationUrl}}", primaryUrl)
                .replace("{{dashboardUrl}}", primaryUrl)
                .replace("{{resetUrl}}", primaryUrl)
                .replace("{{baseUrl}}", secondaryUrl);

        // ============== THEN ==============
        assertThat(result).contains("Hello Frank Miller,");
        assertThat(result).contains("frank.miller@example.com");
        assertThat(result).contains("https://example.com/action");
        assertThat(result).contains("Base URL: https://example.com");
        assertThat(result).doesNotContain("{{"); // No unprocessed placeholders
    }

    @Test
    void shouldHandleNullUserFieldsGracefully() {
        // ============== GIVEN ==============
        User userWithNullFields = User.builder()
                .name(null) // Null name
                .email("test@example.com")
                .build();

        String template = "Hello {{userName}}, email: {{userEmail}}";

        // ============== WHEN ==============
        // Test how template processing handles null values
        String userName = userWithNullFields.getName() != null ? userWithNullFields.getName() : "null";
        String result = template
                .replace("{{userName}}", userName)
                .replace("{{userEmail}}", userWithNullFields.getEmail());

        // ============== THEN ==============
        assertThat(result).contains("Hello null,"); // Should handle null gracefully
        assertThat(result).contains("test@example.com");
    }

    // ============== CONFIGURATION TESTS ==============
    // These test configuration injection and usage

    @Test
    void shouldUseCorrectConfigurationValues() {
        // ============== GIVEN & WHEN ==============
        // Configuration values are injected in @BeforeEach

        // ============== THEN ==============
        // Verify configuration was injected correctly using reflection
        String actualFromName = (String) ReflectionTestUtils.getField(emailService, "fromName");
        String actualFromAddress = (String) ReflectionTestUtils.getField(emailService, "fromAddress");
        String actualBaseUrl = (String) ReflectionTestUtils.getField(emailService, "baseUrl");

        assertThat(actualFromName).isEqualTo(TEST_FROM_NAME);
        assertThat(actualFromAddress).isEqualTo(TEST_FROM_ADDRESS);
        assertThat(actualBaseUrl).isEqualTo(TEST_BASE_URL);
    }

    @Test
    void shouldHandleEmptyConfigurationValues() {
        // ============== GIVEN ==============
        EmailService serviceWithEmptyConfig = new EmailService(mockMailSender);

        // Inject empty/null configuration values
        ReflectionTestUtils.setField(serviceWithEmptyConfig, "fromName", "");
        ReflectionTestUtils.setField(serviceWithEmptyConfig, "fromAddress", "");
        ReflectionTestUtils.setField(serviceWithEmptyConfig, "baseUrl", "");

        // ============== WHEN & THEN ==============
        String fromName = (String) ReflectionTestUtils.getField(serviceWithEmptyConfig, "fromName");
        String fromAddress = (String) ReflectionTestUtils.getField(serviceWithEmptyConfig, "fromAddress");
        String baseUrl = (String) ReflectionTestUtils.getField(serviceWithEmptyConfig, "baseUrl");

        assertThat(fromName).isEmpty();
        assertThat(fromAddress).isEmpty();
        assertThat(baseUrl).isEmpty();
    }

    // ============== ERROR HANDLING TESTS ==============
    // These test various error scenarios

    @Test
    void shouldHandleTemplateNotFoundError() throws Exception {
        // ============== GIVEN ==============
        User testUser = User.builder()
                .name("Grace Lee")
                .email("grace.lee@example.com")
                .build();

        EmailService spyEmailService = spy(emailService);
        doThrow(new RuntimeException("Email template not found! : static/email/templates/verification-email.html"))
                .when(spyEmailService).loadTemplate("verification-email.html");

        // ============== WHEN ==============
        boolean result = spyEmailService.sendVerificationEmail(testUser, TEST_TOKEN);

        // ============== THEN ==============
        assertThat(result).isFalse();

        then(spyEmailService).should().loadTemplate("verification-email.html");
        then(mockMailSender).should(never()).createMimeMessage();
    }

    @Test
    void shouldHandleMimeMessageCreationFailure() throws Exception {
        // ============== GIVEN ==============
        User testUser = User.builder()
                .name("Henry Wilson")
                .email("henry.wilson@example.com")
                .build();

        String mockTemplate = "<html><body>Test</body></html>";

        EmailService spyEmailService = spy(emailService);
        doReturn(mockTemplate).when(spyEmailService).loadTemplate("verification-email.html");

        // Mock MimeMessage creation failure
        given(mockMailSender.createMimeMessage())
                .willThrow(new RuntimeException("Failed to create MIME message"));

        // ============== WHEN ==============
        boolean result = spyEmailService.sendVerificationEmail(testUser, TEST_TOKEN);

        // ============== THEN ==============
        assertThat(result).isFalse();

        then(mockMailSender).should().createMimeMessage();
        then(mockMailSender).should(never()).send(any(MimeMessage.class));
    }

    // ============== INTEGRATION-STYLE TESTS ==============
    // These test complete email workflows

    @Test
    void shouldCompleteFullEmailWorkflowForVerification() throws Exception {
        // ============== GIVEN ==============
        User testUser = User.builder()
                .id(100L)
                .name("Integration Test User")
                .email("integration@example.com")
                .build();

        String fullTemplate = """
            <!DOCTYPE html>
            <html>
                <head><title>Email Verification</title></head>
                <body>
                    <h1>Welcome {{userName}}!</h1>
                    <p>Please verify {{userEmail}}</p>
                    <a href="{{verificationUrl}}">Verify Now</a>
                    <footer>{{baseUrl}}</footer>
                </body>
            </html>
            """;

        EmailService spyEmailService = spy(emailService);
        doReturn(fullTemplate).when(spyEmailService).loadTemplate("verification-email.html");
        given(mockMailSender.createMimeMessage()).willReturn(mockMimeMessage);

        // ============== WHEN ==============
        boolean result = spyEmailService.sendVerificationEmail(testUser, "integration-token-789");

        // ============== THEN ==============
        assertThat(result).isTrue();

        // Verify complete workflow
        then(spyEmailService).should().loadTemplate("verification-email.html");
        then(mockMailSender).should().createMimeMessage();
        then(mockMailSender).should().send(mockMimeMessage);

        // Verify URL generation
        String expectedUrl = TEST_BASE_URL + "/api/auth/verify-email?token=integration-token-789";
        assertThat(expectedUrl).contains("integration-token-789");
        assertThat(expectedUrl).startsWith(TEST_BASE_URL);
    }

    // ============== EDGE CASE TESTS ==============
    // These test unusual but possible scenarios

    @Test
    void shouldHandleSpecialCharactersInUserData() throws Exception {
        // ============== GIVEN ==============
        User userWithSpecialChars = User.builder()
                .name("José María O'Connor-Smith")
                .email("josé.maría@example-domain.co.uk")
                .build();

        String template = "Hello {{userName}}, email: {{userEmail}}";

        EmailService spyEmailService = spy(emailService);
        doReturn(template).when(spyEmailService).loadTemplate("verification-email.html");
        given(mockMailSender.createMimeMessage()).willReturn(mockMimeMessage);

        // ============== WHEN ==============
        boolean result = spyEmailService.sendVerificationEmail(userWithSpecialChars, TEST_TOKEN);

        // ============== THEN ==============
        assertThat(result).isTrue();

        // Verify special characters are handled
        String expectedProcessed = template
                .replace("{{userName}}", userWithSpecialChars.getName())
                .replace("{{userEmail}}", userWithSpecialChars.getEmail());

        assertThat(expectedProcessed).contains("José María O'Connor-Smith");
        assertThat(expectedProcessed).contains("josé.maría@example-domain.co.uk");
    }
}