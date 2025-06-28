package com.friendavailability.service;

import com.friendavailability.model.User;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from-name:LinkUp Team}")
    private String fromName;

    @Value("${app.email.from-address}")
    private String fromAddress;

    @Value("${app.email.verification.base-url:http://localhost:8080}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        System.out.println("EmailService initialized successfully");
    }

    public boolean sendVerificationEmail(User user, String token) {
        try {
            System.out.println("Preparing verification email for: " + user.getEmail());

            String verificationUrl = baseUrl + "/api/auth/verify-email?token=" + token;

            String htmlContent = loadTemplate("verification-email.html");
            htmlContent = processTemplate(htmlContent, user, verificationUrl, baseUrl);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(user.getEmail());
            helper.setSubject("Welcome to LinkUp! Please verify your email");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            System.out.println("Verification email sent successfully to: " + user.getEmail());
            return true;

        } catch (Exception e) {
            System.err.println("Failed to send verification email to: " + user.getEmail());
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendWelcomeEmail(User user) {
        try {
            System.out.println("Preparing welcome email for: " + user.getEmail());

            String dashboardUrl = baseUrl + "/dashboard.html";

            String htmlContent = loadTemplate("welcome-email.html");
            htmlContent = processTemplate(htmlContent, user, dashboardUrl, baseUrl);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(user.getEmail());
            helper.setSubject("Welcome to LinkUp! Your account is ready");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            System.out.println("Welcome email sent successfully to: " + user.getEmail());
            return true;

        } catch (Exception e) {
            System.err.println("Failed to send welcome email to: " + user.getEmail());
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String loadTemplate(String templateName) throws Exception {
        String templatePath = "static/email/templates/" + templateName;
        ClassPathResource resource = new ClassPathResource(templatePath);

        if (!resource.exists()) {
            throw new RuntimeException("Email template not found! : " + templatePath);
        }

        byte[] templateBytes = StreamUtils.copyToByteArray(resource.getInputStream());
        return new String(templateBytes, StandardCharsets.UTF_8);
    }

    private String processTemplate(String template, User user, String primaryUrl, String secondaryUrl) {
        return template
                .replace("{{userName}}", user.getName())
                .replace("{{userEmail}}", user.getEmail())
                .replace("{{verificationUrl}}", primaryUrl)
                .replace("{{dashboardUrl}}", primaryUrl)
                .replace("{{resetUrl}}", primaryUrl)
                .replace("{{baseUrl}}", secondaryUrl);
    }

    public boolean sendPasswordResetEmail(User user, String token) {
        try {
            System.out.println("Preparing password reset email for: " + user.getEmail());

            String resetUrl = baseUrl + "/reset-password?token=" + token;

            String htmlContent = loadTemplate("password-reset-email.html");
            htmlContent = processTemplate(htmlContent, user, resetUrl, baseUrl);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(user.getEmail());
            helper.setSubject("Reset Your LinkUp Password");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            System.out.println("Password reset email sent successfully to: " + user.getEmail());
            return true;

        } catch (Exception e) {
            System.err.println("Failed to send password reset email to: " + user.getEmail());
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }
}