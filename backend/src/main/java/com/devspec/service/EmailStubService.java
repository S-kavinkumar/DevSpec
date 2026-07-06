package com.devspec.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailStubService {
    private static final Logger logger = LoggerFactory.getLogger(EmailStubService.class);

    public void sendWelcomeEmail(String toEmail, String username) {
        logger.info("STUB EMAIL SENT [Welcome] - To: {}, User: {}. Body: Welcome to DEVSPEC, the advanced software architecture reviewer platform!", toEmail, username);
    }

    public void sendVerificationEmail(String toEmail, String token) {
        logger.info("STUB EMAIL SENT [Account Verification] - To: {}. Body: Please verify your account using link: http://localhost:8080/api/auth/verify?token={}", toEmail, token);
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        logger.info("STUB EMAIL SENT [Password Reset] - To: {}. Body: Reset your password using token: {}", toEmail, token);
    }

    public void sendAnalysisCompletedEmail(String toEmail, String projectName, Long reportId, double overallScore) {
        logger.info("STUB EMAIL SENT [Analysis Completed] - To: {}. Body: Your DEVSPEC analysis run for project '{}' has finished successfully! Overall Score: {}%. View report: http://localhost:5173/report/{}", toEmail, projectName, overallScore, reportId);
    }
}
